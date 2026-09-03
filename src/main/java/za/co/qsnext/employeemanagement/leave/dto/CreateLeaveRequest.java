package za.co.qsnext.employeemanagement.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLeaveRequest(

        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @NotBlank(message = "Leave type is required")
        String leaveType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}