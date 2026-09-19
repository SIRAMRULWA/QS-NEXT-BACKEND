package za.co.qsnext.employeemanagement.email;

import java.util.UUID;

/**
 * The RabbitMQ message payload. Deliberately just the id: the consumer
 * re-fetches the current row from the database rather than trusting
 * whatever was true when the message was published, which is both
 * simpler and what makes the idempotency check (skip if already SENT)
 * meaningful.
 */
public record EmailMessage(UUID emailId) {
}
