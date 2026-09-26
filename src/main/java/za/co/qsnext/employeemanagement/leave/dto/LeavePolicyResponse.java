package za.co.qsnext.employeemanagement.leave.dto;

import za.co.qsnext.employeemanagement.leave.LeavePolicy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeavePolicyResponse(
        UUID id,
        String leaveType,
        BigDecimal annualDays,
        String accrualMethod,
        BigDecimal carryOverMaxDays,
        boolean active,
        OffsetDateTime updatedAt
) {

    public static LeavePolicyResponse from(LeavePolicy policy) {
        return new LeavePolicyResponse(
                policy.getId(),
                policy.getLeaveType(),
                policy.getAnnualDays(),
                policy.getAccrualMethod(),
                policy.getCarryOverMaxDays(),
                policy.isActive(),
                policy.getUpdatedAt()
        );
    }
}
