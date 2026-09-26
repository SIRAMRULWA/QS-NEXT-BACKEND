package za.co.qsnext.employeemanagement.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * One row per reminder already sent, keyed by what it was about, so the
 * daily job never nags anyone twice about the same thing.
 */
@Entity
@Table(name = "reminder_log")
public class ReminderLog {

    @Id
    @Column(name = "reminder_key", nullable = false, updatable = false, length = 200)
    private String reminderKey;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private OffsetDateTime sentAt;

    protected ReminderLog() {
        // Required by JPA
    }

    public ReminderLog(String reminderKey) {
        this.reminderKey = reminderKey;
    }

    @PrePersist
    protected void onCreate() {
        sentAt = OffsetDateTime.now();
    }
}
