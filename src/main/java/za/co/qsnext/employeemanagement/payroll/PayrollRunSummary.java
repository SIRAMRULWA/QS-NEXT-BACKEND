package za.co.qsnext.employeemanagement.payroll;

import java.math.BigDecimal;

/**
 * Spring Data interface projection for the org-wide payroll-run
 * summary aggregate query used by the Analytics and Reporting modules.
 */
public interface PayrollRunSummary {

    BigDecimal getTotalEarnings();

    BigDecimal getTotalDeductions();

    BigDecimal getTotalEmployerContributions();

    BigDecimal getTotalNetPay();

    Long getEmployeeCount();
}
