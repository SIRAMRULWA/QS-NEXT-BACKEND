package za.co.qsnext.employeemanagement.timesheet.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSelfServiceTimesheetRequest(

        @NotNull(message = "Period start date is required")
        LocalDate periodStart,

        @NotNull(message = "Period end date is required")
        LocalDate periodEnd
) {
}
