package za.co.qsnext.employeemanagement.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollSummaryAnalyticsResponse(
        UUID payrollRunId,
        BigDecimal totalEarnings,
        BigDecimal totalDeductions,
        BigDecimal totalEmployerContributions,
        BigDecimal totalNetPay,
        long employeeCount
) {
}
