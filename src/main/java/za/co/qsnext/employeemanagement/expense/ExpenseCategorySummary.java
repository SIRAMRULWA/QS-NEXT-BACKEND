package za.co.qsnext.employeemanagement.expense;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data interface projection for the per-category reporting
 * aggregate query.
 */
public interface ExpenseCategorySummary {

    UUID getCategoryId();

    BigDecimal getTotalAmount();

    Long getClaimCount();
}
