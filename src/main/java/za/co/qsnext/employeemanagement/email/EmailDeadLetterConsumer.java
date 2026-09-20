package za.co.qsnext.employeemanagement.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
