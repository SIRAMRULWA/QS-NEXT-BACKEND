package za.co.qsnext.employeemanagement.email;

/**
 * Thrown by an {@link EmailSender} when delivery fails. Deliberately
 * unchecked so it propagates naturally out of the
 * {@code @RabbitListener} method to trigger the consumer's retry/DLQ
 * handling.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
