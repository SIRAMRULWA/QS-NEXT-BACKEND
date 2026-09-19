package za.co.qsnext.employeemanagement.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaxConfigurationRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Line item type is required")
        @Pattern(
                regexp = "DEDUCTION|EMPLOYER_CONTRIBUTION",
                message = "Line item type must be one of DEDUCTION, EMPLOYER_CONTRIBUTION"
        )
        String lineItemType
) {
}
