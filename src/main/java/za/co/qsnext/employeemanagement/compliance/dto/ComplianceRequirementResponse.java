package za.co.qsnext.employeemanagement.compliance.dto;

import za.co.qsnext.employeemanagement.compliance.ComplianceRequirement;

import java.util.UUID;

public record ComplianceRequirementResponse(
        UUID id,
        String name,
        String description,
        String category,
        boolean mandatory,
        Integer validityPeriodDays,
        boolean active
) {

    public static ComplianceRequirementResponse from(ComplianceRequirement requirement) {
        return new ComplianceRequirementResponse(
                requirement.getId(),
                requirement.getName(),
                requirement.getDescription(),
                requirement.getCategory(),
                requirement.isMandatory(),
                requirement.getValidityPeriodDays(),
                requirement.isActive()
        );
    }
}
