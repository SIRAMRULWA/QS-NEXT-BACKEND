package za.co.qsnext.employeemanagement.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpsertPayrollProfileRequest(

        @NotNull(message = "Base salary is required")
        @DecimalMin(value = "0.00", message = "Base salary cannot be negative")
        BigDecimal baseSalary,

        @NotNull(message = "Pay frequency is required")
        @Pattern(
                regexp = "MONTHLY|BIWEEKLY|WEEKLY",
                message = "Pay frequency must be one of MONTHLY, BIWEEKLY, WEEKLY"
        )
        String payFrequency,

        @DecimalMin(value = "0.00", message = "Standard hours cannot be negative")
        BigDecimal standardHoursPerPeriod,

        @DecimalMin(value = "0.00", message = "Overtime hourly rate cannot be negative")
        BigDecimal overtimeHourlyRate,

        @Size(max = 100, message = "Bank account reference must not exceed 100 characters")
        String bankAccountReference,

        @Size(max = 50, message = "Tax number must not exceed 50 characters")
        String taxNumber
) {
}
