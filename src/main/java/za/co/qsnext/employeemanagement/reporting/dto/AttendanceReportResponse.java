package za.co.qsnext.employeemanagement.reporting.dto;

import java.util.UUID;

public record AttendanceReportResponse(

        UUID employeeId,

        String employeeNumber,

        String employeeName,

        long daysPresent,

        long daysAbsent,

        long daysLate,

        long halfDays,

        long leaveDays,

        long remoteDays
) {
}