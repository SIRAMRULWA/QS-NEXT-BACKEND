package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing shift or shift assignment - see
 * {@link OnboardingNotFoundException} for why this isn't split per entity.
 */
public class SchedulingNotFoundException extends RuntimeException {

    public SchedulingNotFoundException(String message) {
        super(message);
    }
}
