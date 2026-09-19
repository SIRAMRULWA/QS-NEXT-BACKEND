package za.co.qsnext.employeemanagement.expense;

import java.math.BigDecimal;

/**
 * Spring Data interface projection for the org-wide expense-trend
 * aggregate query used by the Analytics module.
 */
public interface ExpenseTrendSummary {

    BigDecimal getTotalAmount();

    Long getClaimCount();
}
