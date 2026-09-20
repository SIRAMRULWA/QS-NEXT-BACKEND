package za.co.qsnext.employeemanagement.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.rabbitmq.RabbitMqConstants;

/**
 * Consumes queued emails and hands them to the configured
 * {@link EmailSender}. A failure is recorded on the {@link Email} row and
 * then rethrown so the listener container's retry advice
 * (see {@code RabbitMqConfig}) can retry with backoff before finally
 * routing the message to the dead-letter queue.
 */
@Component
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    private final EmailRepository emailRepository;
    private final EmailSender emailSender;

    public EmailConsumer(
            EmailRepository emailRepository,
            EmailSender emailSender
    ) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
    }

    /**
     * REQUIRES_NEW, not decorative: this listener's own save() calls each
     * open their own transaction via the repository proxy's default
     * REQUIRED propagation, which is normally fine on a thread with no
     * transaction already bound to it - but RabbitMQ listener container
     * threads are pooled and reused across many message deliveries over
     * the container's lifetime, and this diagnosis (a diagnostic build
     * that unconditionally logged the outbox sweep's own tick confirmed
     * it runs every second and never once finds this email PENDING - it
     * leaves PENDING almost immediately and simply never reaches SENT,
     * with no exception logged anywhere) matches the same failure shape
     * already found and fixed in NotificationConsumer: a REQUIRED-
     * propagation write on a reused thread can silently join leftover
     * transaction synchronization state from that thread's own prior
     * work instead of opening a genuinely new transaction, so the write
     * executes but is never actually committed by anything. The
     * exhausted-retries path this queue's own deliveryFailure test
     * deliberately drives (RejectAndDontRequeueRecoverer, nested
     * ListenerExecutionFailedExceptions) runs on this exact same pooled
     * thread immediately beforehand in every observed run - exactly the
     * kind of abnormal, exception-heavy control flow most likely to
     * leave that state behind. REQUIRES_NEW removes the ambiguity: every
     * delivery gets a transaction that is unquestionably its own.
     */
    /*
     * noRollbackFor is required, not optional: the catch block below
     * deliberately records markFailed() and then rethrows so the
     * listener container's retry advice can see the exception and act
     * on it (see class javadoc) - but @Transactional's default rule
     * rolls back on any unchecked exception leaving the method, which
     * would silently undo that same markFailed() write. Without this,
     * EmailConsumerTest's and EmailOutboxIntegrationTest's own
     * assertions that a failed delivery is actually recorded as FAILED
     * (with an attempt count) before the retry/DLQ path continues would
     * stop being true.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = EmailDeliveryException.class
    )
    @RabbitListener(queues = RabbitMqConstants.EMAIL_QUEUE)
    public void handle(EmailMessage message) {

        Email email = emailRepository.findById(message.emailId()).orElse(null);

        if (email == null) {
            log.warn("Received email message for unknown email id {}", message.emailId());
            return;
        }

        if (email.getStatus() == EmailStatus.SENT) {
            /*
             * Idempotency: a redelivery (e.g. after a consumer crash
             * between commit and ack) of an email that was already sent
             * must not send it a second time.
             */
            log.debug("Email {} was already sent; skipping redelivery", email.getId());
            return;
        }

        try {

            email.markProcessing();
            emailRepository.save(email);

            emailSender.send(email);

            email.markSent();
            emailRepository.save(email);

        } catch (ObjectOptimisticLockingFailureException ex) {

            /*
             * Another concurrent delivery of this same message (a broker
             * redelivery, or the outbox sweep racing the original
             * after-commit publish) already advanced this row past the
             * version read here - that delivery is handling it, or
             * already has. Rethrowing would send an otherwise-healthy
             * email through the retry/DLQ path for no reason other than
             * losing a benign race, so this delivery simply has nothing
             * left to do.
             */
            log.debug("Email {} was concurrently updated by another delivery; skipping", email.getId());

        } catch (EmailDeliveryException ex) {

            log.warn("Failed to send email {} (attempt {}): {}",
                    email.getId(), email.getAttemptCount(), ex.getMessage());

            email.markFailed(ex.getMessage());

            try {
                emailRepository.save(email);
            } catch (ObjectOptimisticLockingFailureException lockEx) {
                // Same benign race as above, just reached from the failure
                // path instead of the success path - another concurrent
                // delivery already recorded its own outcome for this row.
                log.debug("Email {} was concurrently updated while recording failure; skipping", email.getId());
            }

            throw ex;
        }
    }
}
