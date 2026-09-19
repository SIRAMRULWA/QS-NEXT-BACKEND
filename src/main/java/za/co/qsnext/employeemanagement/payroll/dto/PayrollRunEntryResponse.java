package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.PayrollRunEntry;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollRunEntryResponse(
        UUID id,
        UUID payrollRunId,
        UUID employeeId,
        BigDecimal totalEarnings,
        BigDecimal totalDeductions,
        BigDecimal totalEmployerContributions,
        BigDecimal netPay
) {

    public static PayrollRunEntryResponse from(PayrollRunEntry entry) {
        return new PayrollRunEntryResponse(
                entry.getId(), entry.getPayrollRunId(), entry.getEmployeeId(), entry.getTotalEarnings(),
                entry.getTotalDeductions(), entry.getTotalEmployerContributions(), entry.getNetPay()
        );
    }
}
