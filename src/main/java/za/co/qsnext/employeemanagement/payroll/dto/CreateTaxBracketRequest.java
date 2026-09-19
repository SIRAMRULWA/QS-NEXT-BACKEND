package za.co.qsnext.employeemanagement.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTaxBracketRequest(

        @NotNull(message = "Minimum amount is required")
        @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative")
        BigDecimal minAmount,

        /**
         * Null means no upper bound (the top bracket).
         */
        BigDecimal maxAmount,

        @NotNull(message = "Rate percent is required")
        @DecimalMin(value = "0.0", message = "Rate percent cannot be negative")
        @DecimalMax(value = "100.0", message = "Rate percent cannot exceed 100")
        BigDecimal ratePercent,

        @NotNull(message = "Effective-from date is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {
}
