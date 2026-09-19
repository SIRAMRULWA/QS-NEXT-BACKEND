package za.co.qsnext.employeemanagement.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The single call business modules make to notify a user of something,
 * without needing to know how that notification is actually delivered
 * (in-app row, email, or both - see {@link NotificationConsumer}).
 */
@Service
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public NotificationPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message
    ) {
        eventPublisher.publishEvent(
                new NotificationEvent(recipientUserId, type, title, message)
        );
    }
}
