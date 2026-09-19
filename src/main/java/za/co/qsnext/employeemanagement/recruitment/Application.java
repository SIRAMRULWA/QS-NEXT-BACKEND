package za.co.qsnext.employeemanagement.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A candidate's application to a specific job posting - this row's
 * {@link #status} IS the candidate pipeline / recruitment stage.
 */
@Entity
@Table(name = "applications")
public class Application {

    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_SCREENING = "SCREENING";
    public static final String STATUS_INTERVIEWING = "INTERVIEWING";
    public static final String STATUS_OFFER = "OFFER";
    public static final String STATUS_HIRED = "HIRED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "job_posting_id", nullable = false, updatable = false)
    private UUID jobPostingId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private OffsetDateTime appliedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Application() {
        // Required by JPA
    }

    public Application(UUID candidateId, UUID jobPostingId) {
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
        this.status = STATUS_APPLIED;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        appliedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public UUID getJobPostingId() {
        return jobPostingId;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Whether this application is still moving through the pipeline
     * (not yet hired, rejected or withdrawn). Callers (see
     * {@code RecruitmentService}) check this before calling any of the
     * transition methods below - these methods themselves don't guard
     * preconditions, consistent with every other entity in this codebase.
     */
    public boolean isActive() {
        return !(STATUS_HIRED.equals(status)
                || STATUS_REJECTED.equals(status)
                || STATUS_WITHDRAWN.equals(status));
    }

    public void advanceToScreening() {
        this.status = STATUS_SCREENING;
    }

    public void advanceToInterviewing() {
        this.status = STATUS_INTERVIEWING;
    }

    public void advanceToOffer() {
        this.status = STATUS_OFFER;
    }

    public void markHired() {
        this.status = STATUS_HIRED;
    }

    public void reject(String reason) {
        this.status = STATUS_REJECTED;
        this.rejectionReason = reason;
    }

    public void withdraw() {
        this.status = STATUS_WITHDRAWN;
    }
}
