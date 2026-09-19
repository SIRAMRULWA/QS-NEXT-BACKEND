package za.co.qsnext.employeemanagement.scheduling;

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

@Entity
@Table(name = "shift_assignments")
public class ShiftAssignment {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "shift_id", nullable = false, updatable = false)
    private UUID shiftId;

    @Column(name = "work_date", nullable = false, updatable = false)
    private LocalDate workDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ShiftAssignment() {
        // Required by JPA
    }

    public ShiftAssignment(
            UUID employeeId,
            UUID shiftId,
            LocalDate workDate,
            UUID createdBy
    ) {
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.workDate = workDate;
        this.createdBy = createdBy;
        this.status = STATUS_SCHEDULED;
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

    public UUID getShiftId() {
        return shiftId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public String getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
    }
}
