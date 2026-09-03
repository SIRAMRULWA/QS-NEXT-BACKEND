package za.co.qsnext.employeemanagement.timesheet.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TimesheetApprovalRequest(

        @NotNull(message = "Approver ID is required")
        UUID approverId
) {
}