package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.expense.ExpenseClaim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseReportResponse(
        UUID employeeId,
        int year,
        int totalClaims,
        int approvedClaims,
        int reimbursedClaims,
        int rejectedClaims,
        BigDecimal totalReimbursedAmount,
        List<ExpenseClaimSummary> claims
) {

    public static ExpenseReportResponse from(UUID employeeId, int year, List<ExpenseClaim> claims) {

        int approved = 0;
        int reimbursed = 0;
        int rejected = 0;
        BigDecimal reimbursedAmount = BigDecimal.ZERO;

        for (ExpenseClaim claim : claims) {
            switch (claim.getStatus()) {
                case ExpenseClaim.STATUS_APPROVED -> approved++;
                case ExpenseClaim.STATUS_REIMBURSED -> {
                    reimbursed++;
                    reimbursedAmount = reimbursedAmount.add(claim.getAmount());
                }
                case ExpenseClaim.STATUS_REJECTED -> rejected++;
                default -> {
                    // DRAFT/SUBMITTED - not yet a final outcome.
                }
            }
        }

        return new ExpenseReportResponse(
                employeeId, year, claims.size(), approved, reimbursed, rejected, reimbursedAmount,
                claims.stream().map(ExpenseClaimSummary::from).toList());
    }

    public record ExpenseClaimSummary(
            UUID id,
            UUID categoryId,
            BigDecimal amount,
            String status,
            LocalDate expenseDate
    ) {

        public static ExpenseClaimSummary from(ExpenseClaim claim) {
            return new ExpenseClaimSummary(
                    claim.getId(), claim.getCategoryId(), claim.getAmount(),
                    claim.getStatus(), claim.getExpenseDate());
        }
    }
}
