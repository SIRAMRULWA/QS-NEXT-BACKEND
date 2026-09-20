package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.EmployeePayrollProfile;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeePayrollProfileResponse(
        UUID id,
        UUID employeeId,
        BigDecimal baseSalary,
        String payFrequency,
        BigDecimal standardHoursPerPeriod,
        BigDecimal overtimeHourlyRate,
        String bankAccountReference,
        String taxNumber,
        boolean active
) {

    public static EmployeePayrollProfileResponse from(EmployeePayrollProfile profile) {
        return new EmployeePayrollProfileResponse(
                profile.getId(), profile.getEmployeeId(), profile.getBaseSalary(), profile.getPayFrequency(),
                profile.getStandardHoursPerPeriod(), profile.getOvertimeHourlyRate(),
                profile.getBankAccountReference(), profile.getTaxNumber(), profile.isActive()
        );
    }
}
