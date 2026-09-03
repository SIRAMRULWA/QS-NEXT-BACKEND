package za.co.qsnext.employeemanagement.leave.dto;

import za.co.qsnext.employeemanagement.leave.LeaveBalance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveBalanceResponse(
        UUID id,
        UUID employeeId,
        String leaveType,
        Integer leaveYear,
        BigDecimal allocatedDays,
        BigDecimal usedDays,
        BigDecimal remainingDays,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {

    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
                balance.getId(),
                balance.getEmployeeId(),
                balance.getLeaveType(),
                balance.getLeaveYear(),
                balance.getAllocatedDays(),
                balance.getUsedDays(),
                balance.getRemainingDays(),
                balance.getCreatedAt(),
                balance.getUpdatedAt(),
                balance.getVersion()
        );
    }
}