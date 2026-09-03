package za.co.qsnext.employeemanagement.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserId(
            UUID userId,
            Pageable pageable
    );

    Page<AuditLog> findByEntityTypeAndEntityId(
            String entityType,
            UUID entityId,
            Pageable pageable
    );

    Page<AuditLog> findByAction(
            String action,
            Pageable pageable
    );
}