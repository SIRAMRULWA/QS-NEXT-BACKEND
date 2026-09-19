package za.co.qsnext.employeemanagement.email;

import java.util.UUID;

/**
 * Published (in the same transaction as the {@link Email} row's insert)
 * when an email is queued. {@link EmailOutboxEventListener} republishes it
 * to RabbitMQ only after that transaction actually commits - the
 * transactional-outbox pattern: the database write is the source of
 * truth, the broker publish is a best-effort notification of it, and
 * {@link EmailOutboxSweepScheduler} is the safety net for when that
 * publish is lost (e.g. the broker was unreachable at the time).
 */
public record EmailQueuedEvent(UUID emailId) {
}
