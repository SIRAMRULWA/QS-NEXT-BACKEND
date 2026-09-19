package za.co.qsnext.employeemanagement.notification;

/**
 * Extension point for a future mobile/web push provider (e.g. FCM, APNs,
 * web push). Deliberately no implementation yet - push delivery needs a
 * device-token registry this backend doesn't have, so wiring a concrete
 * sender is future work (see the Mobile API and Integrations phases). A
 * provider plugs in here the same way {@code EmailSender} does for email,
 * without {@link NotificationConsumer} needing to change.
 */
public interface PushNotificationSender {

    void send(NotificationEvent event);
}
