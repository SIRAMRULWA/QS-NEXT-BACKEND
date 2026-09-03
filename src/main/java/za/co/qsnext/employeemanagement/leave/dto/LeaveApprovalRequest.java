package za.co.qsnext.employeemanagement.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeaveApprovalRequest(

        @NotNull(message = "Approver ID is required")
        UUID approverId
) {
}