package za.co.qsnext.employeemanagement.compliance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateComplianceRequirementRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        @Pattern(
                regexp = "DOCUMENT|POLICY_ACKNOWLEDGEMENT|TRAINING|OTHER",
                message = "Category must be one of DOCUMENT, POLICY_ACKNOWLEDGEMENT, TRAINING, OTHER"
        )
        String category,

        boolean mandatory,

        @Min(value = 1, message = "Validity period must be at least 1 day")
        Integer validityPeriodDays
) {
}
