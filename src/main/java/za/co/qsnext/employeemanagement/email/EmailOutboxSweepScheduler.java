package za.co.qsnext.employeemanagement.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Safety net for the transactional outbox: republishes any email still
 * PENDING longer than expected, on the assumption that the
 * after-commit publish either never fired (e.g. the app crashed between
 * commit and publish) or was lost (the broker was briefly unreachable).
 * Ordinary successful sends never reach this path - they are already
 * PROCESSING/SENT/FAILED well before the threshold.
 */
@Component
public class EmailOutboxSweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxSweepScheduler.class);

    private final EmailRepository emailRepository;
    private final EmailOutboxProducer emailOutboxProducer;
    private final Duration stuckThreshold;
    private final int batchSize;

    public EmailOutboxSweepScheduler(
            EmailRepository emailRepository,
            EmailOutboxProducer emailOutboxProducer,
            @Value("${email.outbox.stuck-threshold-seconds}") long stuckThresholdSeconds,
            @Value("${email.outbox.sweep-batch-size}") int batchSize
    ) {
        this.emailRepository = emailRepository;
        this.emailOutboxProducer = emailOutboxProducer;
        this.stuckThreshold = Duration.ofSeconds(stuckThresholdSeconds);
        this.batchSize = batchSize;
    }

    /**
     * The whole body is deliberately wrapped: {@code java.util.concurrent
     * .ScheduledExecutorService}'s {@code scheduleWithFixedDelay} silently
     * cancels all future runs of a task whose Runnable ever throws, and
     * while Spring's own {@code @Scheduled} wrapper is meant to guard
     * against exactly that, this is a fixed-delay task that runs for the
     * lifetime of the whole test suite's shared Spring context - one
     * transient failure (the broker being briefly unreachable while the
     * suite's other ~40 test classes are hammering it, or racing Flyway
     * on a very early first tick) permanently silencing the safety net
     * for the rest of the run would defeat the entire point of having
     * one. A single email's publish failing here isn't fatal - it just
     * means this row is still PENDING and stays eligible for the next
     * tick to pick back up.
     */
    @Scheduled(fixedDelayString = "${email.outbox.sweep-interval-ms}")
    public void sweep() {

        // TEMPORARY: unconditional per-tick trace to settle, empirically,
        // whether @Scheduled is invoking this method at all in the real
        // integration-test context - every attempt to explain the outbox
        // sweep's complete silence (raising the stuck threshold, wrapping
        // the body in try/catch) has produced zero change in observable
        // behavior, which is only consistent with the method never being
        // called in the first place. To be removed once that's confirmed
        // one way or the other.
        log.info("Outbox sweep tick");

        try {

            OffsetDateTime threshold = OffsetDateTime.now().minus(stuckThreshold);

            List<Email> stuckEmails = emailRepository.findByStatusAndCreatedAtBefore(
                    EmailStatus.PENDING,
                    threshold,
                    PageRequest.of(0, batchSize)
            );

            if (stuckEmails.isEmpty()) {
                return;
            }

            log.info("Outbox sweep republishing {} email(s) stuck in PENDING", stuckEmails.size());

            for (Email email : stuckEmails) {
                try {
                    emailOutboxProducer.publish(email.getId());
                } catch (RuntimeException ex) {
                    log.warn("Outbox sweep failed to republish email {}; it remains PENDING and will be retried next sweep", email.getId(), ex);
                }
            }

        } catch (RuntimeException ex) {
            log.warn("Outbox sweep tick failed; will retry on the next tick", ex);
        }
    }
}
