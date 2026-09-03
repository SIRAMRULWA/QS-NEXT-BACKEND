package za.co.qsnext.employeemanagement.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.notification.dto.NotificationResponse;

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