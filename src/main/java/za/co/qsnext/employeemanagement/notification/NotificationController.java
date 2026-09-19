package za.co.qsnext.employeemanagement.notification;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.notification.dto.NotificationPreferenceResponse;
import za.co.qsnext.employeemanagement.notification.dto.NotificationResponse;
import za.co.qsnext.employeemanagement.notification.dto.UpdateNotificationPreferenceRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("""
            hasAuthority('NOTIFICATION_READ')
            and @notificationAuthorizationService
                .canAccessUserNotifications(#userId, authentication)
            """)
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                notificationService.getByUser(
                        userId,
                        pageable
                )
        );
    }

    @PreAuthorize("""
            hasAuthority('NOTIFICATION_READ')
            and @notificationAuthorizationService
                .canAccessUserNotifications(#userId, authentication)
            """)
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnread(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                notificationService.getUnread(
                        userId,
                        pageable
                )
        );
    }

    @PreAuthorize("""
            hasAuthority('NOTIFICATION_MARK_READ')
            and @notificationAuthorizationService
                .canAccessNotification(#notificationId, authentication)
            """)
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId
    ) {

        return ResponseEntity.ok(
                notificationService.markAsRead(
                        notificationId
                )
        );
    }

    @PreAuthorize("""
            hasAuthority('NOTIFICATION_MARK_UNREAD')
            and @notificationAuthorizationService
                .canAccessNotification(#notificationId, authentication)
            """)
    @PatchMapping("/{notificationId}/unread")
    public ResponseEntity<NotificationResponse> markAsUnread(
            @PathVariable UUID notificationId
    ) {

        return ResponseEntity.ok(
                notificationService.markAsUnread(
                        notificationId
                )
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getOwnPreference(
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                notificationService.getPreference(userDetails.getUserId())
        );
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updateOwnPreference(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                notificationService.updatePreference(
                        userDetails.getUserId(),
                        request.inAppEnabled(),
                        request.emailEnabled()
                )
        );
    }

    private Pageable createPageable(
            int page,
            int size
    ) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }
}