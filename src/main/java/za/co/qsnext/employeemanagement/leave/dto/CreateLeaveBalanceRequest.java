package za.co.qsnext.employeemanagement.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLeaveBalanceRequest(

        @NotNull
        UUID employeeId,

        @NotBlank
        String leaveType,

        @NotNull
        Integer leaveYear,

        @NotNull
        @DecimalMin(value = "0.0")
        BigDecimal allocatedDays
) {
}