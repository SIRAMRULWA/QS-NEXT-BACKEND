package za.co.qsnext.employeemanagement.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.NotificationNotFoundException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;
import za.co.qsnext.employeemanagement.notification.dto.NotificationResponse;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<NotificationResponse> getByUser(
            UUID userId,
            Pageable pageable
    ) {
        verifyUserExists(userId);

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    public Page<NotificationResponse> getUnread(
            UUID userId,
            Pageable pageable
    ) {
        verifyUserExists(userId);

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId,
                        pageable
                )
                .map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(
            UUID notificationId
    ) {
        Notification notification = getNotification(notificationId);

        if (notification.isRead()) {
            return NotificationResponse.from(notification);
        }

        notification.markAsRead();

        return NotificationResponse.from(
                notificationRepository.save(notification)
        );
    }

    @Transactional
    public NotificationResponse markAsUnread(
            UUID notificationId
    ) {
        Notification notification = getNotification(notificationId);

        if (!notification.isRead()) {
            return NotificationResponse.from(notification);
        }

        notification.markAsUnread();

        return NotificationResponse.from(
                notificationRepository.save(notification)
        );
    }

    private Notification getNotification(UUID notificationId) {

        return notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new NotificationNotFoundException(
                                "Notification not found: " + notificationId
                        )
                );
    }

    private void verifyUserExists(UUID userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(
                    "User not found: " + userId
            );
        }
    }
}