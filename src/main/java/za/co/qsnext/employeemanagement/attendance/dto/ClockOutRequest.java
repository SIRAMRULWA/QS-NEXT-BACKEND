package za.co.qsnext.employeemanagement.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClockOutRequest(

        @NotNull(message = "Attendance ID is required")
        UUID attendanceId
) {
}