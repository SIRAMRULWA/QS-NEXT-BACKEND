package za.co.qsnext.employeemanagement.expense.dto;

import za.co.qsnext.employeemanagement.expense.ExpenseClaim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseClaimResponse(
        UUID id,
        UUID employeeId,
        UUID categoryId,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate expenseDate,
        UUID receiptDocumentId,
        String status,
        OffsetDateTime submittedAt,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        UUID rejectedBy,
        OffsetDateTime rejectedAt,
        String rejectionReason,
        OffsetDateTime reimbursedAt
) {

    public static ExpenseClaimResponse from(ExpenseClaim claim) {
        return new ExpenseClaimResponse(
                claim.getId(), claim.getEmployeeId(), claim.getCategoryId(), claim.getAmount(),
                claim.getCurrency(), claim.getDescription(), claim.getExpenseDate(), claim.getReceiptDocumentId(),
                claim.getStatus(), claim.getSubmittedAt(), claim.getApprovedBy(), claim.getApprovedAt(),
                claim.getRejectedBy(), claim.getRejectedAt(), claim.getRejectionReason(), claim.getReimbursedAt()
        );
    }
}
