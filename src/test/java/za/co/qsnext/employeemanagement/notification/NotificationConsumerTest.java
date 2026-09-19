package za.co.qsnext.employeemanagement.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    private NotificationConsumer notificationConsumer;

    @BeforeEach
    void setUp() {
        notificationConsumer = new NotificationConsumer(
                notificationRepository, notificationPreferenceRepository, userRepository, emailService);
    }

    private User userWithId(String email) {
        User user = new User("jane.doe", email, "hash");
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }

    @Test
    void onNotification_createsInAppRowAndQueuesEmail_whenNoPreferenceIsSet() {
        User user = userWithId("jane.doe@qsnext.co.za");

        when(notificationPreferenceRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        notificationConsumer.onNotification(new NotificationEvent(
                user.getId(), NotificationType.LEAVE_REQUEST_APPROVED, "Title", "Message"));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(notificationCaptor.getValue().getType())
                .isEqualTo(NotificationType.LEAVE_REQUEST_APPROVED.name());

        verify(emailService).queueEmail(
                eq(EmailTemplate.NOTIFICATION),
                eq("jane.doe@qsnext.co.za"),
                eq(Map.of("title", "Title", "message", "Message"))
        );
    }

    @Test
    void onNotification_skipsInAppRow_whenInAppIsDisabled() {
        User user = userWithId("jane.doe@qsnext.co.za");

        NotificationPreference preference = new NotificationPreference(user.getId());
        preference.update(false, true);

        when(notificationPreferenceRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(preference));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        notificationConsumer.onNotification(new NotificationEvent(
                user.getId(), NotificationType.TIMESHEET_APPROVED, "Title", "Message"));

        verify(notificationRepository, never()).save(any());
        verify(emailService).queueEmail(any(), any(), any());
    }

    @Test
    void onNotification_skipsEmail_whenEmailIsDisabled() {
        User user = userWithId("jane.doe@qsnext.co.za");

        NotificationPreference preference = new NotificationPreference(user.getId());
        preference.update(true, false);

        when(notificationPreferenceRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(preference));

        notificationConsumer.onNotification(new NotificationEvent(
                user.getId(), NotificationType.TIMESHEET_REJECTED, "Title", "Message"));

        verify(notificationRepository).save(any());
        verify(emailService, never()).queueEmail(any(), any(), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void onNotification_respectsBothPreferencesDisabled() {
        User user = userWithId("jane.doe@qsnext.co.za");

        NotificationPreference preference = new NotificationPreference(user.getId());
        preference.update(false, false);

        when(notificationPreferenceRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(preference));

        notificationConsumer.onNotification(new NotificationEvent(
                user.getId(), NotificationType.LEAVE_REQUEST_REJECTED, "Title", "Message"));

        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).queueEmail(any(), any(), any());
    }
}
