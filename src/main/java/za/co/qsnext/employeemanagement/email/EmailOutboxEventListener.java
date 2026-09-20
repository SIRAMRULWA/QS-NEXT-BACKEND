package za.co.qsnext.employeemanagement.email;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailOutboxEventListener {

    private final EmailOutboxProducer emailOutboxProducer;

    public EmailOutboxEventListener(EmailOutboxProducer emailOutboxProducer) {
        this.emailOutboxProducer = emailOutboxProducer;
    }

    /**
     * Fires only once the transaction that queued the email has actually
     * committed, off the request thread. If the broker happens to be
     * unreachable at this moment, the email row stays PENDING and
     * {@link EmailOutboxSweepScheduler} will retry the publish later -
     * this is a best-effort notification, not the source of truth.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailQueued(EmailQueuedEvent event) {
        emailOutboxProducer.publish(event.emailId());
    }
}
