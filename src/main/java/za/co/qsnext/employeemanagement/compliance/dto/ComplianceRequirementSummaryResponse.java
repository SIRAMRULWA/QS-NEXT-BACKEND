package za.co.qsnext.employeemanagement.compliance.dto;

import java.util.UUID;

public record ComplianceRequirementSummaryResponse(
        UUID requirementId,
        long pendingCount,
        long completedCount,
        long expiredCount
) {
}
