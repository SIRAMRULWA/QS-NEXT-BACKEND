package za.co.qsnext.employeemanagement.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveReportResponse(

        UUID employeeId,

        String employeeNumber,

        String employeeName,

        String leaveType,

        BigDecimal allocatedDays,

        BigDecimal usedDays,

        BigDecimal remainingDays,

        long pendingRequests,

        long approvedRequests,

        long rejectedRequests
) {
}