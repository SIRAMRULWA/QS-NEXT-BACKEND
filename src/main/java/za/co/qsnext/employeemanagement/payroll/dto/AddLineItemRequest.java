package za.co.qsnext.employeemanagement.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A manually-added line item (bonus, allowance, ad-hoc deduction, ...)
 * on a still-DRAFT payroll run entry - see PayrollRunService#addLineItem.
 */
public record AddLineItemRequest(

        @NotNull(message = "Type is required")
        @Pattern(
                regexp = "EARNING|DEDUCTION|EMPLOYER_CONTRIBUTION",
                message = "Type must be one of EARNING, DEDUCTION, EMPLOYER_CONTRIBUTION"
        )
        String type,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Amount is required")
        BigDecimal amount
) {
}
