package za.co.qsnext.employeemanagement.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An offer made against a specific {@link Application}. Accepting an
 * offer is the candidate's decision (recorded here); actually converting
 * them into an {@code Employee} and starting their onboarding is a
 * separate, explicit HR action - see {@code OfferService#hire}.
 */
@Entity
@Table(name = "offers")
public class Offer {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true, updatable = false)
    private UUID applicationId;

    @Column(name = "job_title", nullable = false, length = 200, updatable = false)
    private String jobTitle;

    @Column(name = "salary_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal salaryAmount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Offer() {
        // Required by JPA
    }

    public Offer(
            UUID applicationId,
            String jobTitle,
            BigDecimal salaryAmount,
            String currency,
            LocalDate startDate
    ) {
        this.applicationId = applicationId;
        this.jobTitle = jobTitle;
        this.salaryAmount = salaryAmount;
        this.currency = currency;
        this.startDate = startDate;
        this.status = STATUS_DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public BigDecimal getSalaryAmount() {
        return salaryAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    public boolean isSent() {
        return STATUS_SENT.equals(status);
    }

    public boolean isAccepted() {
        return STATUS_ACCEPTED.equals(status);
    }

    public void send() {
        this.status = STATUS_SENT;
        this.sentAt = OffsetDateTime.now();
    }

    public void accept() {
        this.status = STATUS_ACCEPTED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void decline() {
        this.status = STATUS_DECLINED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void withdraw() {
        this.status = STATUS_WITHDRAWN;
    }
}
