package za.co.qsnext.employeemanagement.performance;

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
 * One employee's review within a {@link PerformanceCycle}, combining a
 * self-assessment and a manager assessment. The rating scale is
 * deliberately just a plain integer - what it means (1-5, 1-10, ...) is
 * a configuration/convention decision for the caller, not hardcoded here.
 */
@Entity
@Table(name = "performance_reviews")
public class PerformanceReview {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cycle_id", nullable = false, updatable = false)
    private UUID cycleId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "reviewer_user_id", nullable = false, updatable = false)
    private UUID reviewerUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "self_rating")
    private Integer selfRating;

    @Column(name = "self_comments", length = 2000)
    private String selfComments;

    @Column(name = "self_submitted_at")
    private OffsetDateTime selfSubmittedAt;

    @Column(name = "manager_rating")
    private Integer managerRating;

    @Column(name = "manager_comments", length = 2000)
    private String managerComments;

    @Column(name = "manager_submitted_at")
    private OffsetDateTime managerSubmittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PerformanceReview() {
        // Required by JPA
    }

    public PerformanceReview(UUID cycleId, UUID employeeId, UUID reviewerUserId) {
        this.cycleId = cycleId;
        this.employeeId = employeeId;
        this.reviewerUserId = reviewerUserId;
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

    public UUID getCycleId() {
        return cycleId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getReviewerUserId() {
        return reviewerUserId;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSelfRating() {
        return selfRating;
    }

    public String getSelfComments() {
        return selfComments;
    }

    public OffsetDateTime getSelfSubmittedAt() {
        return selfSubmittedAt;
    }

    public Integer getManagerRating() {
        return managerRating;
    }

    public String getManagerComments() {
        return managerComments;
    }

    public OffsetDateTime getManagerSubmittedAt() {
        return managerSubmittedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isFullyCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public void submitSelfAssessment(Integer rating, String comments) {
        this.selfRating = rating;
        this.selfComments = comments;
        this.selfSubmittedAt = OffsetDateTime.now();
        refreshStatus();
    }

    public void submitManagerAssessment(Integer rating, String comments) {
        this.managerRating = rating;
        this.managerComments = comments;
        this.managerSubmittedAt = OffsetDateTime.now();
        refreshStatus();
    }

    private void refreshStatus() {
        if (selfSubmittedAt != null && managerSubmittedAt != null) {
            this.status = STATUS_COMPLETED;
        } else if (selfSubmittedAt != null || managerSubmittedAt != null) {
            this.status = STATUS_IN_PROGRESS;
        } else {
            this.status = STATUS_DRAFT;
        }
    }
}
