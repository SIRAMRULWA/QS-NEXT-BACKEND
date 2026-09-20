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
 * A goal (or measurable KPI - deliberately the same entity, since a KPI
 * is just a goal with a numeric target described in its text) tracked
 * for one employee within a {@link PerformanceCycle}.
 */
@Entity
@Table(name = "performance_goals")
public class PerformanceGoal {

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
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

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PerformanceGoal() {
        // Required by JPA
    }

    public PerformanceGoal(
            UUID cycleId,
            UUID employeeId,
            String title,
            String description,
            LocalDate targetDate
    ) {
        this.cycleId = cycleId;
        this.employeeId = employeeId;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = STATUS_NOT_STARTED;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public void updateStatus(String status) {
        this.status = status;
    }
}
