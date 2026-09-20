package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailRepository emailRepository;
    @Mock
    private EmailSender emailSender;

    private EmailConsumer emailConsumer;

    @BeforeEach
    void setUp() {
        emailConsumer = new EmailConsumer(emailRepository, emailSender);
    }

    private Email pendingEmail() {
        Email email = new Email("jane.doe@qsnext.co.za", "Subject", "Body", "PASSWORD_RESET");
        setId(email, UUID.randomUUID());
        return email;
    }

    @Test
    void handle_marksTheEmailSent_whenDeliverySucceeds() {
        Email email = pendingEmail();
        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));

        emailConsumer.handle(new EmailMessage(email.getId()));

        assertThat(email.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(email.getAttemptCount()).isEqualTo(1);
        verify(emailSender).send(email);
    }

    @Test
    void handle_marksTheEmailFailedAndRethrows_whenDeliveryFails() {
        Email email = pendingEmail();
        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));

        EmailDeliveryException failure = new EmailDeliveryException("SMTP unavailable", null);
        org.mockito.Mockito.doThrow(failure).when(emailSender).send(email);

        assertThatThrownBy(() -> emailConsumer.handle(new EmailMessage(email.getId())))
                .isSameAs(failure);

        assertThat(email.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(email.getFailureReason()).isEqualTo("SMTP unavailable");
    }

    @Test
    void handle_isIdempotent_skippingAnEmailThatWasAlreadySent() {
        Email email = pendingEmail();
        email.markSent();
        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));

        emailConsumer.handle(new EmailMessage(email.getId()));

        verify(emailSender, never()).send(any());
    }

    @Test
    void handle_skipsGracefully_whenAnotherConcurrentDeliveryWonTheRace() {
        Email email = pendingEmail();
        when(emailRepository.findById(email.getId())).thenReturn(Optional.of(email));
        org.mockito.Mockito.doThrow(new ObjectOptimisticLockingFailureException(Email.class, email.getId()))
                .when(emailRepository).save(email);

        emailConsumer.handle(new EmailMessage(email.getId()));

        verify(emailSender, never()).send(any());
    }

    @Test
    void handle_doesNothing_whenTheEmailNoLongerExists() {
        UUID emailId = UUID.randomUUID();
        when(emailRepository.findById(emailId)).thenReturn(Optional.empty());

        emailConsumer.handle(new EmailMessage(emailId));

        verify(emailSender, never()).send(any());
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
