package za.co.qsnext.employeemanagement.compliance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRecordResponse;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRequirementResponse;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRequirementSummaryResponse;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.ComplianceNotFoundException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private ComplianceRequirementRepository requirementRepository;
    @Mock
    private ComplianceRecordRepository recordRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private AuditService auditService;

    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        complianceService = new ComplianceService(
                requirementRepository, recordRepository, employeeRepository,
                documentRepository, notificationPublisher, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private ComplianceRequirement requirementWithId(UUID id, Integer validityPeriodDays) {
        ComplianceRequirement requirement = new ComplianceRequirement(
                "ID Document", "Desc", ComplianceRequirement.CATEGORY_DOCUMENT, true, validityPeriodDays);
        setId(requirement, id);
        return requirement;
    }

    private ComplianceRecord recordWithId(UUID id, UUID employeeId, UUID requirementId) {
        ComplianceRecord record = new ComplianceRecord(employeeId, requirementId);
        setId(record, id);
        return record;
    }

    @Test
    void createRequirement_savesTheRequirement() {
        when(requirementRepository.existsByName("ID Document")).thenReturn(false);
        when(requirementRepository.save(any())).thenAnswer(invocation -> {
            ComplianceRequirement requirement = invocation.getArgument(0);
            setId(requirement, UUID.randomUUID());
            return requirement;
        });

        ComplianceRequirementResponse response = complianceService.createRequirement(
                "ID Document", "Desc", ComplianceRequirement.CATEGORY_DOCUMENT, true, 365);

        assertThat(response.name()).isEqualTo("ID Document");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRequirement_rejectsADuplicateName() {
        when(requirementRepository.existsByName("ID Document")).thenReturn(true);

        assertThatThrownBy(() -> complianceService.createRequirement(
                "ID Document", "Desc", ComplianceRequirement.CATEGORY_DOCUMENT, true, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void assignToEmployee_createsARecordAndNotifiesTheEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(requirementRepository.findById(requirementId))
                .thenReturn(Optional.of(requirementWithId(requirementId, 365)));
        when(recordRepository.existsByEmployeeIdAndRequirementIdAndStatus(
                employeeId, requirementId, ComplianceRecord.STATUS_PENDING)).thenReturn(false);
        when(recordRepository.save(any())).thenAnswer(invocation -> {
            ComplianceRecord record = invocation.getArgument(0);
            setId(record, UUID.randomUUID());
            return record;
        });

        ComplianceRecordResponse response = complianceService.assignToEmployee(employeeId, requirementId);

        assertThat(response.status()).isEqualTo(ComplianceRecord.STATUS_PENDING);
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.COMPLIANCE_TASK_ASSIGNED), any(), any());
    }

    @Test
    void assignToEmployee_throws_whenRequirementIsInactive() {
        UUID employeeId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        ComplianceRequirement requirement = requirementWithId(requirementId, null);
        requirement.deactivate();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(requirementRepository.findById(requirementId)).thenReturn(Optional.of(requirement));

        assertThatThrownBy(() -> complianceService.assignToEmployee(employeeId, requirementId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assignToEmployee_rejectsADuplicatePendingRecord() {
        UUID employeeId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(requirementRepository.findById(requirementId))
                .thenReturn(Optional.of(requirementWithId(requirementId, null)));
        when(recordRepository.existsByEmployeeIdAndRequirementIdAndStatus(
                employeeId, requirementId, ComplianceRecord.STATUS_PENDING)).thenReturn(true);

        assertThatThrownBy(() -> complianceService.assignToEmployee(employeeId, requirementId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assignToEmployee_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.assignToEmployee(employeeId, UUID.randomUUID()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void assignToEmployee_throws_whenRequirementDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(requirementRepository.findById(requirementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.assignToEmployee(employeeId, requirementId))
                .isInstanceOf(ComplianceNotFoundException.class);
    }

    @Test
    void getRecordsForEmployee_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> complianceService.getRecordsForEmployee(employeeId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void completeRecord_computesExpiry_fromRequirementValidityPeriod() {
        UUID recordId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        ComplianceRecord record = recordWithId(recordId, employeeId, requirementId);

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(requirementRepository.findById(requirementId))
                .thenReturn(Optional.of(requirementWithId(requirementId, 30)));

        ComplianceRecordResponse response =
                complianceService.completeRecord(recordId, userId, false, null, "Done");

        assertThat(response.status()).isEqualTo(ComplianceRecord.STATUS_COMPLETED);
        assertThat(response.expiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
    }

    @Test
    void completeRecord_leavesExpiryNull_whenRequirementHasNoValidityPeriod() {
        UUID recordId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();

        ComplianceRecord record = recordWithId(recordId, employeeId, requirementId);

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(requirementRepository.findById(requirementId))
                .thenReturn(Optional.of(requirementWithId(requirementId, null)));

        ComplianceRecordResponse response =
                complianceService.completeRecord(recordId, UUID.randomUUID(), true, null, null);

        assertThat(response.expiresAt()).isNull();
    }

    @Test
    void completeRecord_isIdempotent_whenAlreadyCompleted() {
        UUID recordId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        ComplianceRecord record = recordWithId(recordId, UUID.randomUUID(), requirementId);
        record.complete(null, "Done", UUID.randomUUID(), null);

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ComplianceRecordResponse response =
                complianceService.completeRecord(recordId, UUID.randomUUID(), true, null, "Ignored");

        assertThat(response.status()).isEqualTo(ComplianceRecord.STATUS_COMPLETED);
        assertThat(response.notes()).isEqualTo("Done");
    }

    @Test
    void completeRecord_throws_whenEvidenceDocumentBelongsToAnotherEmployee() {
        UUID recordId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID evidenceDocumentId = UUID.randomUUID();
        ComplianceRecord record = recordWithId(recordId, employeeId, UUID.randomUUID());

        Document evidence = new Document(
                UUID.randomUUID(), "ID_DOCUMENT", "ID", null, UUID.randomUUID(), 1,
                "key", "id.pdf", "application/pdf", 10, null, UUID.randomUUID());

        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(documentRepository.findById(evidenceDocumentId)).thenReturn(Optional.of(evidence));

        assertThatThrownBy(() -> complianceService.completeRecord(
                recordId, UUID.randomUUID(), true, evidenceDocumentId, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getExpiringRecords_rejectsANegativeWindow() {
        assertThatThrownBy(() -> complianceService.getExpiringRecords(-5))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getRequirementSummary_countsEachStatus() {
        UUID requirementId = UUID.randomUUID();

        when(requirementRepository.existsById(requirementId)).thenReturn(true);
        when(recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_PENDING))
                .thenReturn(2L);
        when(recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_COMPLETED))
                .thenReturn(5L);
        when(recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_EXPIRED))
                .thenReturn(1L);

        ComplianceRequirementSummaryResponse summary = complianceService.getRequirementSummary(requirementId);

        assertThat(summary.pendingCount()).isEqualTo(2);
        assertThat(summary.completedCount()).isEqualTo(5);
        assertThat(summary.expiredCount()).isEqualTo(1);
    }

    @Test
    void getRequirementSummary_throws_whenRequirementDoesNotExist() {
        UUID requirementId = UUID.randomUUID();
        when(requirementRepository.existsById(requirementId)).thenReturn(false);

        assertThatThrownBy(() -> complianceService.getRequirementSummary(requirementId))
                .isInstanceOf(ComplianceNotFoundException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
