package za.co.qsnext.employeemanagement.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateLeaveBalanceRequest(

        @NotNull(message = "Allocated days is required")
        @DecimalMin(value = "0", message = "Allocated days cannot be negative")
        BigDecimal allocatedDays
) {
}
