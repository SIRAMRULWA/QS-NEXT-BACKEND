package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxSweepSchedulerTest {

    @Mock
    private EmailRepository emailRepository;
    @Mock
    private EmailOutboxProducer emailOutboxProducer;

    private EmailOutboxSweepScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EmailOutboxSweepScheduler(
                emailRepository, emailOutboxProducer, 120, 50);
    }

    private Email pendingEmail() {
        Email email = new Email("jane.doe@qsnext.co.za", "Subject", "Body", "PASSWORD_RESET");
        setId(email, UUID.randomUUID());
        return email;
    }

    @Test
    void sweep_republishesEveryStuckEmail() {
        Email first = pendingEmail();
        Email second = pendingEmail();

        when(emailRepository.findByStatusAndCreatedAtBefore(
                org.mockito.ArgumentMatchers.eq(EmailStatus.PENDING),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));

        scheduler.sweep();

        verify(emailOutboxProducer).publish(first.getId());
        verify(emailOutboxProducer).publish(second.getId());
    }

    @Test
    void sweep_doesNothing_whenNoEmailsAreStuck() {
        when(emailRepository.findByStatusAndCreatedAtBefore(
                org.mockito.ArgumentMatchers.eq(EmailStatus.PENDING),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of());

        scheduler.sweep();

        verify(emailOutboxProducer, never()).publish(any());
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
