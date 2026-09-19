package za.co.qsnext.employeemanagement.compliance.dto;

import za.co.qsnext.employeemanagement.compliance.ComplianceRecord;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComplianceRecordResponse(
        UUID id,
        UUID employeeId,
        UUID requirementId,
        String status,
        UUID evidenceDocumentId,
        String notes,
        OffsetDateTime completedAt,
        OffsetDateTime expiresAt,
        UUID reviewedBy,
        OffsetDateTime createdAt
) {

    public static ComplianceRecordResponse from(ComplianceRecord record) {
        return new ComplianceRecordResponse(
                record.getId(),
                record.getEmployeeId(),
                record.getRequirementId(),
                record.getStatus(),
                record.getEvidenceDocumentId(),
                record.getNotes(),
                record.getCompletedAt(),
                record.getExpiresAt(),
                record.getReviewedBy(),
                record.getCreatedAt()
        );
    }
}
