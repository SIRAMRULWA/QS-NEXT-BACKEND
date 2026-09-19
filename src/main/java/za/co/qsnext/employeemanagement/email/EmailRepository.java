package za.co.qsnext.employeemanagement.email;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EmailRepository
        extends JpaRepository<Email, UUID> {

    Page<Email> findByStatus(
            EmailStatus status,
            Pageable pageable
    );

    /**
     * Emails still PENDING despite being queued a while ago - the outbox
     * event that should have published them either never fired or was
     * lost (e.g. the broker was briefly unreachable).
     */
    List<Email> findByStatusAndCreatedAtBefore(
            EmailStatus status,
            OffsetDateTime threshold,
            Pageable pageable
    );
}