package za.co.qsnext.employeemanagement.exception;

/**
 * Covers both a missing {@code SignatureRequest} and a missing signer
 * within one, mirroring how {@code OnboardingNotFoundException} covers
 * templates, workflows and tasks.
 */
public class SignatureRequestNotFoundException extends RuntimeException {

    public SignatureRequestNotFoundException(String message) {
        super(message);
    }
}
