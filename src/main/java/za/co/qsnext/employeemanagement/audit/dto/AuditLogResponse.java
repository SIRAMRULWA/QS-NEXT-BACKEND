package za.co.qsnext.employeemanagement.audit.dto;

import za.co.qsnext.employeemanagement.audit.AuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        String ipAddress,
        String result,
        OffsetDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getIpAddress(),
                auditLog.getResult(),
                auditLog.getCreatedAt()
        );
    }
}
