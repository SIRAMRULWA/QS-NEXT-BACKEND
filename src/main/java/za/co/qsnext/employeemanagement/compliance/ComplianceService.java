package za.co.qsnext.employeemanagement.compliance;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ComplianceService {

    private static final String REQUIREMENT_ENTITY_TYPE = "ComplianceRequirement";
    private static final String RECORD_ENTITY_TYPE = "ComplianceRecord";

    private final ComplianceRequirementRepository requirementRepository;
    private final ComplianceRecordRepository recordRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditService auditService;

    public ComplianceService(
            ComplianceRequirementRepository requirementRepository,
            ComplianceRecordRepository recordRepository,
            EmployeeRepository employeeRepository,
            DocumentRepository documentRepository,
            NotificationPublisher notificationPublisher,
            AuditService auditService
    ) {
        this.requirementRepository = requirementRepository;
        this.recordRepository = recordRepository;
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.notificationPublisher = notificationPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public ComplianceRequirementResponse createRequirement(
            String name,
            String description,
            String category,
            boolean mandatory,
            Integer validityPeriodDays
    ) {
        if (requirementRepository.existsByName(name)) {
            throw new DuplicateResourceException("Compliance requirement already exists: " + name);
        }

        ComplianceRequirement requirement = requirementRepository.save(
                new ComplianceRequirement(name, description, category, mandatory, validityPeriodDays)
        );

        auditService.log(
                "COMPLIANCE_REQUIREMENT_CREATED", REQUIREMENT_ENTITY_TYPE, requirement.getId(),
                AuditService.RESULT_SUCCESS
        );

        return ComplianceRequirementResponse.from(requirement);
    }

    public List<ComplianceRequirementResponse> getActiveRequirements() {
        return requirementRepository.findByActiveTrue().stream()
                .map(ComplianceRequirementResponse::from)
                .toList();
    }

    @Transactional
    public ComplianceRecordResponse assignToEmployee(UUID employeeId, UUID requirementId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        ComplianceRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ComplianceNotFoundException("Compliance requirement not found: " + requirementId));

        if (!requirement.isActive()) {
            throw new BusinessRuleException("Compliance requirement is no longer active: " + requirement.getName());
        }

        if (recordRepository.existsByEmployeeIdAndRequirementIdAndStatus(
                employeeId, requirementId, ComplianceRecord.STATUS_PENDING)) {
            throw new BusinessRuleException(
                    "Employee already has a pending record for this compliance requirement"
            );
        }

        ComplianceRecord record = recordRepository.save(new ComplianceRecord(employeeId, requirementId));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.COMPLIANCE_TASK_ASSIGNED,
                "Compliance task assigned",
                "You have a new compliance task: \"" + requirement.getName() + "\"."
        );

        auditService.log(
                "COMPLIANCE_RECORD_ASSIGNED", RECORD_ENTITY_TYPE, record.getId(), AuditService.RESULT_SUCCESS
        );

        return ComplianceRecordResponse.from(record);
    }

    public List<ComplianceRecordResponse> getRecordsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeRecords(employeeId, requesterUserId, requesterCanManage);

        return recordRepository.findByEmployeeId(employeeId).stream()
                .map(ComplianceRecordResponse::from)
                .toList();
    }

    public List<ComplianceRecordResponse> getMyRecords(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return recordRepository.findByEmployeeId(employee.getId()).stream()
                .map(ComplianceRecordResponse::from)
                .toList();
    }

    @Transactional
    public ComplianceRecordResponse completeRecord(
            UUID recordId,
            UUID requesterUserId,
            boolean requesterCanManage,
            UUID evidenceDocumentId,
            String notes
    ) {
        ComplianceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ComplianceNotFoundException("Compliance record not found: " + recordId));

        assertCanAccessEmployeeRecords(record.getEmployeeId(), requesterUserId, requesterCanManage);

        if (!record.isPending()) {
            return ComplianceRecordResponse.from(record);
        }

        if (evidenceDocumentId != null) {
            Document evidence = documentRepository.findById(evidenceDocumentId)
                    .orElseThrow(() -> new BusinessRuleException("Evidence document not found: " + evidenceDocumentId));

            if (!evidence.getEmployeeId().equals(record.getEmployeeId())) {
                throw new BusinessRuleException("Evidence document does not belong to this employee");
            }
        }

        ComplianceRequirement requirement = requirementRepository.findById(record.getRequirementId())
                .orElseThrow(() -> new ComplianceNotFoundException(
                        "Compliance requirement not found: " + record.getRequirementId()
                ));

        OffsetDateTime expiresAt = requirement.getValidityPeriodDays() == null
                ? null
                : OffsetDateTime.now().plusDays(requirement.getValidityPeriodDays());

        record.complete(evidenceDocumentId, notes, requesterUserId, expiresAt);

        auditService.log(
                "COMPLIANCE_RECORD_COMPLETED", RECORD_ENTITY_TYPE, recordId, AuditService.RESULT_SUCCESS
        );

        return ComplianceRecordResponse.from(record);
    }

    public List<ComplianceRecordResponse> getExpiringRecords(int withinDays) {

        if (withinDays < 0) {
            throw new BusinessRuleException("withinDays cannot be negative");
        }

        OffsetDateTime now = OffsetDateTime.now();

        return recordRepository.findByStatusAndExpiresAtBetween(
                        ComplianceRecord.STATUS_COMPLETED, now, now.plusDays(withinDays)
                )
                .stream()
                .map(ComplianceRecordResponse::from)
                .toList();
    }

    public ComplianceRequirementSummaryResponse getRequirementSummary(UUID requirementId) {

        if (!requirementRepository.existsById(requirementId)) {
            throw new ComplianceNotFoundException("Compliance requirement not found: " + requirementId);
        }

        return new ComplianceRequirementSummaryResponse(
                requirementId,
                recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_PENDING),
                recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_COMPLETED),
                recordRepository.countByRequirementIdAndStatus(requirementId, ComplianceRecord.STATUS_EXPIRED)
        );
    }

    private void assertCanAccessEmployeeRecords(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to access these compliance records");
        }
    }
}
