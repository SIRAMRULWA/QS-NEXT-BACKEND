package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.PayrollRun;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PayrollRunResponse(
        UUID id,
        UUID payPeriodId,
        String status,
        UUID runByUserId,
        UUID approvedByUserId,
        OffsetDateTime approvedAt,
        OffsetDateTime paidAt
) {

    public static PayrollRunResponse from(PayrollRun run) {
        return new PayrollRunResponse(
                run.getId(), run.getPayPeriodId(), run.getStatus(), run.getRunByUserId(),
                run.getApprovedByUserId(), run.getApprovedAt(), run.getPaidAt()
        );
    }
}
