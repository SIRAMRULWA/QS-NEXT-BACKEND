package za.co.qsnext.employeemanagement.attendance.dto;

import za.co.qsnext.employeemanagement.attendance.Attendance;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        LocalDate attendanceDate,
        OffsetDateTime clockIn,
        OffsetDateTime clockOut,
        String status,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployeeId(),
                attendance.getAttendanceDate(),
                attendance.getClockIn(),
                attendance.getClockOut(),
                attendance.getStatus(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt(),
                attendance.getVersion()
        );
    }
}