package za.co.qsnext.employeemanagement.integration;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The "Finance/Payroll integration" step in the Expense architecture
 * (Expense -&gt; Approval -&gt; Finance/Payroll integration). There is no
 * Payroll module yet (a later phase), so {@link InternalPayrollIntegrationProvider}
 * is a logging/audit-only placeholder; a real deployment would plug a
 * provider in here - either the eventual internal Payroll module or an
 * external payroll vendor - without {@code ExpenseService} needing to
 * change.
 */
public interface PayrollIntegrationProvider {

    void notifyExpenseApproved(UUID expenseClaimId, UUID employeeId, BigDecimal amount, String currency);

    void notifyExpenseReimbursed(UUID expenseClaimId, UUID employeeId, BigDecimal amount, String currency);
}
