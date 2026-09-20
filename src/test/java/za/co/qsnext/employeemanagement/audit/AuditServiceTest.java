package za.co.qsnext.employeemanagement.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.audit.dto.AuditLogResponse;
import za.co.qsnext.employeemanagement.observability.BusinessMetrics;
import za.co.qsnext.employeemanagement.security.ClientIpResolver;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private BusinessMetrics businessMetrics;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, businessMetrics, new ClientIpResolver(""));
    }

    @Test
    void getByUser_mapsRepositoryPageToResponseDtos() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog log = new AuditLog(
                userId, "LOGIN_SUCCESS", "USER", userId, null, null,
                "127.0.0.1", "test-agent", AuditService.RESULT_SUCCESS, null);

        when(auditLogRepository.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = auditService.getByUser(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("LOGIN_SUCCESS");
        assertThat(result.getContent().getFirst().userId()).isEqualTo(userId);
    }

    @Test
    void getByEntity_mapsRepositoryPageToResponseDtos() {
        UUID entityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog log = new AuditLog(
                null, "EMAIL_DEAD_LETTERED", "EMAIL", entityId, null, null,
                null, null, AuditService.RESULT_FAILURE, null);

        when(auditLogRepository.findByEntityTypeAndEntityId("EMAIL", entityId, pageable))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = auditService.getByEntity("EMAIL", entityId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().entityType()).isEqualTo("EMAIL");
        assertThat(result.getContent().getFirst().entityId()).isEqualTo(entityId);
    }

    @Test
    void getByAction_mapsRepositoryPageToResponseDtos() {
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog log = new AuditLog(
                null, "TOKEN_REUSE_DETECTED", "USER", UUID.randomUUID(), null, null,
                null, null, AuditService.RESULT_FAILURE, null);

        when(auditLogRepository.findByAction("TOKEN_REUSE_DETECTED", pageable))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = auditService.getByAction("TOKEN_REUSE_DETECTED", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().result()).isEqualTo(AuditService.RESULT_FAILURE);
    }

    @Test
    void log_recordsABusinessMetricForEveryAuditedEvent() {
        UUID userId = UUID.randomUUID();

        auditService.log(userId, "LEAVE_REQUEST_APPROVED", "LeaveRequest", UUID.randomUUID(),
                AuditService.RESULT_SUCCESS);

        verify(businessMetrics).recordEvent("LEAVE_REQUEST_APPROVED", AuditService.RESULT_SUCCESS);
    }
}
