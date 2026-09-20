package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.timesheet.Timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TimesheetReportResponse(
        UUID employeeId,
        int year,
        int totalTimesheets,
        int approvedTimesheets,
        int pendingTimesheets,
        int rejectedTimesheets,
        BigDecimal totalApprovedHours,
        List<TimesheetSummary> timesheets
) {

    public static TimesheetReportResponse from(
            UUID employeeId,
            int year,
            List<Timesheet> timesheets,
            Map<UUID, BigDecimal> hoursByTimesheetId
    ) {

        int approved = 0;
        int pending = 0;
        int rejected = 0;
        BigDecimal approvedHours = BigDecimal.ZERO;

        for (Timesheet timesheet : timesheets) {
            BigDecimal hours = hoursByTimesheetId.getOrDefault(timesheet.getId(), BigDecimal.ZERO);

            switch (timesheet.getStatus()) {
                case "APPROVED" -> {
                    approved++;
                    approvedHours = approvedHours.add(hours);
                }
                case "PENDING" -> pending++;
                case "REJECTED" -> rejected++;
                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }
        }

        List<TimesheetSummary> summaries = timesheets.stream()
                .map(timesheet -> TimesheetSummary.from(
                        timesheet, hoursByTimesheetId.getOrDefault(timesheet.getId(), BigDecimal.ZERO)))
                .toList();

        return new TimesheetReportResponse(
                employeeId, year, timesheets.size(), approved, pending, rejected, approvedHours, summaries);
    }

    public record TimesheetSummary(
            UUID id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status,
            BigDecimal totalHours
    ) {

        public static TimesheetSummary from(Timesheet timesheet, BigDecimal totalHours) {
            return new TimesheetSummary(
                    timesheet.getId(), timesheet.getPeriodStart(), timesheet.getPeriodEnd(),
                    timesheet.getStatus(), totalHours);
        }
    }
}
