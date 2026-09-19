package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailRepository, eventPublisher);
    }

    @Test
    void queueEmail_savesARenderedEmail_andPublishesAQueuedEvent() {
        when(emailRepository.save(any(Email.class)))
                .thenAnswer(invocation -> {
                    Email email = invocation.getArgument(0);
                    setId(email, UUID.randomUUID());
                    return email;
                });

        UUID emailId = emailService.queueEmail(
                EmailTemplate.PASSWORD_RESET,
                "jane.doe@qsnext.co.za",
                Map.of("token", "abc123", "expiresInMinutes", "30")
        );

        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());

        Email saved = emailCaptor.getValue();
        assertThat(saved.getRecipient()).isEqualTo("jane.doe@qsnext.co.za");
        assertThat(saved.getSubject()).isEqualTo(EmailTemplate.PASSWORD_RESET.subject());
        assertThat(saved.getBody()).contains("abc123");
        assertThat(saved.getType()).isEqualTo("PASSWORD_RESET");
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(emailId).isNotNull();

        ArgumentCaptor<EmailQueuedEvent> eventCaptor = ArgumentCaptor.forClass(EmailQueuedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().emailId()).isEqualTo(emailId);
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
