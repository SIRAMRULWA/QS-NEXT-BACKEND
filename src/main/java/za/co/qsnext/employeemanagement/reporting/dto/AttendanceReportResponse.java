package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.attendance.Attendance;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AttendanceReportResponse(
        UUID employeeId,
        int year,
        int totalRecords,
        int presentDays,
        int absentDays,
        int lateDays,
        int halfDays,
        int leaveDays,
        int remoteDays,
        List<AttendanceSummary> records
) {

    public static AttendanceReportResponse from(
            UUID employeeId,
            int year,
            List<Attendance> attendanceRecords
    ) {

        int present = 0;
        int absent = 0;
        int late = 0;
        int halfDay = 0;
        int onLeave = 0;
        int remote = 0;

        List<AttendanceSummary> records =
                attendanceRecords.stream()
                        .map(AttendanceSummary::from)
                        .toList();

        for (Attendance attendance : attendanceRecords) {

            switch (attendance.getStatus()) {

                case "PRESENT" -> present++;

                case "ABSENT" -> absent++;

                case "LATE" -> late++;

                case "HALF_DAY" -> halfDay++;

                case "ON_LEAVE" -> onLeave++;

                case "REMOTE" -> remote++;

                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }
        }

        return new AttendanceReportResponse(
                employeeId,
                year,
                attendanceRecords.size(),
                present,
                absent,
                late,
                halfDay,
                onLeave,
                remote,
                records
        );
    }

    public record AttendanceSummary(
            UUID id,
            LocalDate attendanceDate,
            OffsetDateTime clockIn,
            OffsetDateTime clockOut,
            String status,
            String notes
    ) {

        public static AttendanceSummary from(
                Attendance attendance
        ) {
            return new AttendanceSummary(
                    attendance.getId(),
                    attendance.getAttendanceDate(),
                    attendance.getClockIn(),
                    attendance.getClockOut(),
                    attendance.getStatus(),
                    attendance.getNotes()
            );
        }
    }
}