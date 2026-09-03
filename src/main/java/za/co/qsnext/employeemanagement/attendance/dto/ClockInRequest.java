package za.co.qsnext.employeemanagement.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ClockInRequest(

        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @NotNull(message = "Attendance date is required")
        LocalDate attendanceDate
) {
}