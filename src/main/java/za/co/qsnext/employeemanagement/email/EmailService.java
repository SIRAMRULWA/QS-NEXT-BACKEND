package za.co.qsnext.employeemanagement.email;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Single entry point for queuing an email. This is the transactional
 * outbox write: the {@link Email} row is inserted as part of the caller's
 * own transaction (so it can never exist without whatever business change
 * triggered it, and vice versa), and {@link EmailQueuedEvent} is only
 * actually acted on after that transaction commits.
 */
@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmailService(
            EmailRepository emailRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.emailRepository = emailRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID queueEmail(
            EmailTemplate template,
            String recipient,
            Map<String, String> variables
    ) {

        Email email = new Email(
                recipient,
                template.subject(),
                template.renderBody(variables),
                template.type()
        );

        Email saved = emailRepository.save(email);

        eventPublisher.publishEvent(new EmailQueuedEvent(saved.getId()));

        return saved.getId();
    }
}
