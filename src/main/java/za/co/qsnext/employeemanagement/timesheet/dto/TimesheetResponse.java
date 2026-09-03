package za.co.qsnext.employeemanagement.timesheet.dto;

import za.co.qsnext.employeemanagement.timesheet.Timesheet;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TimesheetResponse(
        UUID id,
        UUID employeeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        OffsetDateTime submittedAt,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {

    public static TimesheetResponse from(Timesheet timesheet) {
        return new TimesheetResponse(
                timesheet.getId(),
                timesheet.getEmployeeId(),
                timesheet.getPeriodStart(),
                timesheet.getPeriodEnd(),
                timesheet.getStatus(),
                timesheet.getSubmittedAt(),
                timesheet.getApprovedBy(),
                timesheet.getApprovedAt(),
                timesheet.getCreatedAt(),
                timesheet.getUpdatedAt(),
                timesheet.getVersion()
        );
    }
}