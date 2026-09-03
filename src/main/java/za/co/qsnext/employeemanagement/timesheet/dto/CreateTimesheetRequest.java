package za.co.qsnext.employeemanagement.timesheet.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTimesheetRequest(

        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @NotNull(message = "Period start is required")
        LocalDate periodStart,

        @NotNull(message = "Period end is required")
        LocalDate periodEnd
) {
}