package za.co.qsnext.employeemanagement.notification.dto;

import za.co.qsnext.employeemanagement.notification.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        String title,
        String message,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {

    public static NotificationResponse from(
            Notification notification
    ) {

        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}