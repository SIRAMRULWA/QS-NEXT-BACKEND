package za.co.qsnext.employeemanagement.expense.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseCategorySummaryResponse(UUID categoryId, BigDecimal totalAmount, long claimCount) {
}
