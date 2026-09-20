package za.co.qsnext.employeemanagement.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

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
            emailRepository.save(email);

            throw ex;
        }
    }
}
