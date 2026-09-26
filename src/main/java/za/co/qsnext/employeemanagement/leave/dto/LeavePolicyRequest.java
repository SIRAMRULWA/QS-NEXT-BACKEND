package za.co.qsnext.employeemanagement.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LeavePolicyRequest(

        @NotBlank(message = "Leave type is required")
        String leaveType,

        @NotNull(message = "Annual days is required")
        @DecimalMin(value = "0", message = "Annual days cannot be negative")
        BigDecimal annualDays,

        @NotBlank(message = "Accrual method is required")
        String accrualMethod,

        @DecimalMin(value = "0", message = "Carry-over days cannot be negative")
        BigDecimal carryOverMaxDays,

        Boolean active
) {
}
