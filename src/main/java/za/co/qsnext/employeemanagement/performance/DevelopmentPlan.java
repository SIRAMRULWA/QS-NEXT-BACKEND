package za.co.qsnext.employeemanagement.performance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A development action for an employee, optionally arising from a
 * {@link PerformanceReview} and optionally recommending a specific
 * Learning module course ({@link #recommendedCourseId}).
 */
@Entity
@Table(name = "development_plans")
public class DevelopmentPlan {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "recommended_course_id")
    private UUID recommendedCourseId;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DevelopmentPlan() {
        // Required by JPA
    }

    public DevelopmentPlan(
            UUID employeeId,
            UUID reviewId,
            String description,
            UUID recommendedCourseId,
            LocalDate targetDate
    ) {
        this.employeeId = employeeId;
        this.reviewId = reviewId;
        this.description = description;
        this.recommendedCourseId = recommendedCourseId;
        this.targetDate = targetDate;
        this.status = STATUS_OPEN;
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

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public String getDescription() {
        return description;
    }

    public UUID getRecommendedCourseId() {
        return recommendedCourseId;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void complete() {
        this.status = STATUS_COMPLETED;
    }
}
