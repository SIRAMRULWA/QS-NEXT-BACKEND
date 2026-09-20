package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing AiSuggestion.
 */
public class AiNotFoundException extends RuntimeException {

    public AiNotFoundException(String message) {
        super(message);
    }
}
