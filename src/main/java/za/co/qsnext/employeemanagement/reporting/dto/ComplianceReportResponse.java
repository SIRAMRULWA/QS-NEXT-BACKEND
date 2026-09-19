package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.compliance.ComplianceRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComplianceReportResponse(
        UUID employeeId,
        int totalRecords,
        int completedRecords,
        int pendingRecords,
        int expiredRecords,
        List<ComplianceRecordSummary> records
) {

    public static ComplianceReportResponse from(UUID employeeId, List<ComplianceRecord> records) {

        int completed = 0;
        int pending = 0;
        int expired = 0;

        for (ComplianceRecord record : records) {
            switch (record.getStatus()) {
                case ComplianceRecord.STATUS_COMPLETED -> completed++;
                case ComplianceRecord.STATUS_PENDING -> pending++;
                case ComplianceRecord.STATUS_EXPIRED -> expired++;
                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }
        }

        return new ComplianceReportResponse(
                employeeId, records.size(), completed, pending, expired,
                records.stream().map(ComplianceRecordSummary::from).toList());
    }

    public record ComplianceRecordSummary(
            UUID id,
            UUID requirementId,
            String status,
            OffsetDateTime completedAt,
            OffsetDateTime expiresAt
    ) {

        public static ComplianceRecordSummary from(ComplianceRecord record) {
            return new ComplianceRecordSummary(
                    record.getId(), record.getRequirementId(), record.getStatus(),
                    record.getCompletedAt(), record.getExpiresAt());
        }
    }
}
