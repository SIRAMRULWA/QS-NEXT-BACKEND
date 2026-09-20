package za.co.qsnext.employeemanagement.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.rabbitmq.RabbitMqConstants;

/**
 * Final stop for an email whose delivery attempts were all exhausted.
 * {@link EmailConsumer} already marks the row FAILED on its last attempt;
 * this exists to make dead-lettering itself visible in the audit trail
 * (distinct from "failed and about to be retried") for operational review.
 */
@Component
public class EmailDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailDeadLetterConsumer.class);

    private final EmailRepository emailRepository;
    private final AuditService auditService;

    public EmailDeadLetterConsumer(
            EmailRepository emailRepository,
            AuditService auditService
    ) {
        this.emailRepository = emailRepository;
        this.auditService = auditService;
    }

    /**
     * REQUIRES_NEW for the same reason as {@link EmailConsumer#handle}: this
     * runs on a pooled RabbitMQ listener container thread that is reused
     * across deliveries, and this listener only ever fires right after an
     * exhausted-retries/DLQ-routing event - exactly the abnormal,
     * exception-heavy control flow most likely to leave stale transaction
     * synchronization state behind on the thread. Without this, the save()
     * below (and auditService.log()'s own write) could silently join that
     * stale state instead of opening a real transaction, so the DLQ row's
     * FAILED status and the EMAIL_DEAD_LETTERED audit entry would never
     * actually commit - with no exception thrown anywhere to reveal it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @RabbitListener(queues = RabbitMqConstants.EMAIL_DEAD_LETTER_QUEUE)
    public void handle(EmailMessage message) {

        log.error("Email {} exhausted its retry attempts and was dead-lettered", message.emailId());

        emailRepository.findById(message.emailId()).ifPresent(email -> {

            if (email.getStatus() != EmailStatus.SENT) {
                email.markFailed("Exhausted retry attempts; moved to the dead-letter queue");
                emailRepository.save(email);
            }
        });

        auditService.log(
                null,
                "EMAIL_DEAD_LETTERED",
                "EMAIL",
                message.emailId(),
                AuditService.RESULT_FAILURE
        );
    }
}
