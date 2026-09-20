package za.co.qsnext.employeemanagement.email;

/**
 * Abstraction over the actual delivery mechanism, so the consumer
 * doesn't depend on SMTP specifically. {@link SmtpEmailSender} is the
 * only implementation today; a future provider (e.g. a transactional
 * email API) plugs in here without touching the outbox/consumer pipeline.
 */
public interface EmailSender {

    void send(Email email) throws EmailDeliveryException;
}
