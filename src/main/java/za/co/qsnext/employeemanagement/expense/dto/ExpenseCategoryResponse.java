package za.co.qsnext.employeemanagement.expense.dto;

import za.co.qsnext.employeemanagement.expense.ExpenseCategory;

import java.util.UUID;

public record ExpenseCategoryResponse(UUID id, String name, String description, boolean active) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
                category.getId(), category.getName(), category.getDescription(), category.isActive()
        );
    }
}
