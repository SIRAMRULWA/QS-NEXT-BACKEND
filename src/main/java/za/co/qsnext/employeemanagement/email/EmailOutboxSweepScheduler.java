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

    @Scheduled(fixedDelayString = "${email.outbox.sweep-interval-ms}")
    public void sweep() {

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

        stuckEmails.forEach(email -> emailOutboxProducer.publish(email.getId()));
    }
}
