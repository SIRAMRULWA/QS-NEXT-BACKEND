package za.co.qsnext.employeemanagement.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emails")
public class Email {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "recipient",
            nullable = false,
            length = 255
    )
    private String recipient;

    @Column(
            name = "subject",
            nullable = false,
            length = 255
    )
    private String subject;

    @Column(
            name = "body",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String body;

    @Column(
            name = "type",
            nullable = false,
            length = 50
    )
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private EmailStatus status;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    protected Email() {
        // Required by JPA
    }

    public Email(
            String recipient,
            String subject,
            String body,
            String type
    ) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.type = type;
        this.status = EmailStatus.PENDING;
        this.attemptCount = 0;
    }

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now =
                OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getType() {
        return type;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void markProcessing() {

        this.status =
                EmailStatus.PROCESSING;

        this.attemptCount++;

        this.lastAttemptAt =
                OffsetDateTime.now();

        this.failureReason = null;
    }

    public void markSent() {

        this.status =
                EmailStatus.SENT;

        this.sentAt =
                OffsetDateTime.now();

        this.failureReason = null;
    }

    public void markFailed(
            String failureReason
    ) {

        this.status =
                EmailStatus.FAILED;

        this.failureReason =
                failureReason;
    }

    public void resetForRetry() {

        this.status =
                EmailStatus.PENDING;

        this.failureReason = null;
    }
}