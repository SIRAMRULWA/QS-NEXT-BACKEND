package za.co.qsnext.employeemanagement.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.audit.dto.AuditLogResponse;

import java.util.UUID;

/**
 * Read-only access to the audit trail. Restricted to AUDIT_READ (granted
 * only to ADMIN by default - see the seed migration) since audit records
 * can reveal who did what across the whole platform.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                auditService.getByUser(userId, createPageable(page, size))
        );
    }

    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLogResponse>> getByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                auditService.getByEntity(entityType, entityId, createPageable(page, size))
        );
    }

    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditLogResponse>> getByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                auditService.getByAction(action, createPageable(page, size))
        );
    }

    private Pageable createPageable(int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
