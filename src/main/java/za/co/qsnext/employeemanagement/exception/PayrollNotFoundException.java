package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing PayPeriod, EmployeePayrollProfile, TaxConfiguration,
 * TaxBracket, PayrollRun or PayrollRunEntry.
 */
public class PayrollNotFoundException extends RuntimeException {

    public PayrollNotFoundException(String message) {
        super(message);
    }
}
