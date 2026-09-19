package za.co.qsnext.employeemanagement.ai;

/**
 * Thrown by an {@link AiProvider} when the underlying vendor call fails
 * for infrastructure reasons (network failure, authentication failure,
 * rate limiting) - never for an ordinary model response the caller
 * simply disagrees with.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
