package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.payroll.PayrollRunEntry;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PayrollReportResponse(
        UUID employeeId,
        int year,
        int payslipCount,
        BigDecimal totalEarnings,
        BigDecimal totalDeductions,
        BigDecimal totalNetPay,
        List<PayslipSummary> payslips
) {

    public static PayrollReportResponse from(UUID employeeId, int year, List<PayrollRunEntry> entries) {

        BigDecimal totalEarnings = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;

        for (PayrollRunEntry entry : entries) {
            totalEarnings = totalEarnings.add(entry.getTotalEarnings());
            totalDeductions = totalDeductions.add(entry.getTotalDeductions());
            totalNetPay = totalNetPay.add(entry.getNetPay());
        }

        return new PayrollReportResponse(
                employeeId, year, entries.size(), totalEarnings, totalDeductions, totalNetPay,
                entries.stream().map(PayslipSummary::from).toList());
    }

    public record PayslipSummary(
            UUID id,
            UUID payrollRunId,
            BigDecimal totalEarnings,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            OffsetDateTime createdAt
    ) {

        public static PayslipSummary from(PayrollRunEntry entry) {
            return new PayslipSummary(
                    entry.getId(), entry.getPayrollRunId(), entry.getTotalEarnings(),
                    entry.getTotalDeductions(), entry.getNetPay(), entry.getCreatedAt());
        }
    }
}
