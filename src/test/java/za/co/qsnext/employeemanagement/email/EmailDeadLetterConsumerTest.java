package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDeadLetterConsumerTest {

    @Mock
    private EmailRepository emailRepository;
    @Mock
    private AuditService auditService;

    private EmailDeadLetterConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EmailDeadLetterConsumer(emailRepository, auditService);
    }

    @Test
    void handle_marksAFailedEmailAsExhausted_andAudits() {
        Email email = new Email("jane.doe@qsnext.co.za", "Subject", "Body", "PASSWORD_RESET");
        setId(email, UUID.randomUUID());
        email.markFailed("SMTP unavailable");

        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));

        consumer.handle(new EmailMessage(email.getId()));

        assertThat(email.getFailureReason()).contains("dead-letter queue");
        verify(emailRepository).save(email);
        verify(auditService).log(
                null, "EMAIL_DEAD_LETTERED", "EMAIL", email.getId(), AuditService.RESULT_FAILURE);
    }

    @Test
    void handle_doesNotOverwriteAnAlreadySentEmail() {
        Email email = new Email("jane.doe@qsnext.co.za", "Subject", "Body", "PASSWORD_RESET");
        setId(email, UUID.randomUUID());
        email.markSent();

        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));

        consumer.handle(new EmailMessage(email.getId()));

        assertThat(email.getStatus()).isEqualTo(EmailStatus.SENT);
        verify(emailRepository, never()).save(email);
    }

    private static void setId(Email email, UUID id) {
        try {
            Field field = Email.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(email, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
