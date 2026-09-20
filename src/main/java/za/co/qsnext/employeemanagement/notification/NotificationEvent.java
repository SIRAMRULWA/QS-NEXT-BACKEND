package za.co.qsnext.employeemanagement.notification;

import java.util.UUID;

/**
 * Published by business modules (leave, timesheets, ...) when something
 * happens that a user should be notified about.
 * {@link NotificationConsumer} turns this into an in-app
 * {@link Notification} row and, if the recipient's preferences allow it,
 * a queued email.
 */
public record NotificationEvent(
        UUID recipientUserId,
        NotificationType type,
        String title,
        String message
) {
}
