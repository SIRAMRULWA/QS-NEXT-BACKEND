package za.co.qsnext.employeemanagement.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A reusable shift definition (e.g. "Morning Shift 08:00-16:00"). Not
 * tied to a specific date - {@link ShiftAssignment} is what puts an
 * employee on a shift for a given day.
 */
@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Shift() {
        // Required by JPA
    }

    public Shift(
            String name,
            LocalTime startTime,
            LocalTime endTime,
            UUID departmentId
    ) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.departmentId = departmentId;
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

    public String getName() {
        return name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Whether this shift crosses midnight (end time earlier than start
     * time on the clock), e.g. a 22:00-06:00 night shift.
     */
    public boolean isOvernight() {
        return endTime.isBefore(startTime);
    }
}
