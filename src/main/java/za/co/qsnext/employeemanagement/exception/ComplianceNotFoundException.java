package za.co.qsnext.employeemanagement.exception;

/**
 * Covers both a missing {@code ComplianceRequirement} and a missing
 * {@code ComplianceRecord}.
 */
public class ComplianceNotFoundException extends RuntimeException {

    public ComplianceNotFoundException(String message) {
        super(message);
    }
}
