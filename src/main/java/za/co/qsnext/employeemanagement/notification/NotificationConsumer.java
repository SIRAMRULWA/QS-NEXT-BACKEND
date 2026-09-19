package za.co.qsnext.employeemanagement.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.Map;

/**
 * Turns a {@link NotificationEvent} into an in-app {@link Notification}
 * row, a queued email if the recipient allows it, and a push
 * notification to any device they have registered (see {@code
 * DeviceRegistryPushNotificationSender} - push has no separate
 * enable/disable preference, since registering a device is itself an
 * opt-in). Runs only after the business transaction that published the
 * event actually commits - e.g. a leave request approval that gets
 * rolled back must not notify the employee that it was approved.
 */
@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationSender pushNotificationSender;

    public NotificationConsumer(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository,
            EmailService emailService,
            PushNotificationSender pushNotificationSender
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pushNotificationSender = pushNotificationSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationEvent event) {

        NotificationPreference preference = notificationPreferenceRepository
                .findByUserId(event.recipientUserId())
                .orElse(null);

        boolean inAppEnabled = preference == null || preference.isInAppEnabled();
        boolean emailEnabled = preference == null || preference.isEmailEnabled();

        if (inAppEnabled) {
            notificationRepository.save(new Notification(
                    event.recipientUserId(),
                    event.type().name(),
                    event.title(),
                    event.message()
            ));
        }

        if (emailEnabled) {
            userRepository.findById(event.recipientUserId())
                    .map(User::getEmail)
                    .ifPresent(recipientEmail -> emailService.queueEmail(
                            EmailTemplate.NOTIFICATION,
                            recipientEmail,
                            Map.of("title", event.title(), "message", event.message())
                    ));
        }

        pushNotificationSender.send(event);
    }
}
