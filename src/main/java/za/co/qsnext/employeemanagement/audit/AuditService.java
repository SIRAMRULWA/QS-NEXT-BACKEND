package za.co.qsnext.employeemanagement.audit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import za.co.qsnext.employeemanagement.audit.dto.AuditLogResponse;
import za.co.qsnext.employeemanagement.security.SecurityUtils;

import java.util.UUID;

/**
 * Records audit trail entries. Writes run in their own transaction
 * ({@code REQUIRES_NEW}) so that an audit record survives even when the
 * business operation it describes (e.g. a failed login, a denied request)
 * rolls back the caller's transaction.
 */
@Service
public class AuditService {

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Logs an event attributed to the currently authenticated user, if any.
     */
    public void log(
            String action,
            String entityType,
            UUID entityId,
            String result
    ) {
        log(
                SecurityUtils.currentUserId().orElse(null),
                action,
                entityType,
                entityId,
                null,
                null,
                result
        );
    }

    public void log(
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            String result
    ) {
        log(userId, action, entityType, entityId, null, null, result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            String oldValues,
            String newValues,
            String result
    ) {
        AuditLog auditLog = new AuditLog(
                userId,
                action,
                entityType,
                entityId,
                oldValues,
                newValues,
                currentRequestIp(),
                currentRequestUserAgent(),
                result
        );

        auditLogRepository.save(auditLog);
    }

    private String currentRequestIp() {

        HttpServletRequest request = currentRequest();

        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String currentRequestUserAgent() {

        HttpServletRequest request = currentRequest();

        return request == null
                ? null
                : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {

        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        return attributes.getRequest();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByEntity(
            String entityType,
            UUID entityId,
            Pageable pageable
    ) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(AuditLogResponse::from);
    }
}
