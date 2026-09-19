package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing ExpenseCategory or ExpenseClaim.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
