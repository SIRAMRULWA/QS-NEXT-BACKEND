package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing onboarding template, workflow, or task - they're
 * different entities but the same kind of client error, so one exception
 * type is enough rather than one per entity.
 */
public class OnboardingNotFoundException extends RuntimeException {

    public OnboardingNotFoundException(String message) {
        super(message);
    }
}
