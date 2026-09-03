package za.co.qsnext.employeemanagement.leave.dto;

import za.co.qsnext.employeemanagement.leave.LeaveRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveResponse(
        UUID id,
        UUID employeeId,
        String leaveType,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {

    public static LeaveResponse from(LeaveRequest leaveRequest) {
        return new LeaveResponse(
                leaveRequest.getId(),
                leaveRequest.getEmployeeId(),
                leaveRequest.getLeaveType(),
                leaveRequest.getStatus(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getApprovedBy(),
                leaveRequest.getApprovedAt(),
                leaveRequest.getCreatedAt(),
                leaveRequest.getUpdatedAt(),
                leaveRequest.getVersion()
        );
    }
}