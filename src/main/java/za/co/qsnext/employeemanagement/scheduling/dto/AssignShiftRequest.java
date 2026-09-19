package za.co.qsnext.employeemanagement.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AssignShiftRequest(

        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @NotNull(message = "Shift ID is required")
        UUID shiftId,

        @NotNull(message = "Work date is required")
        LocalDate workDate
) {
}
