package za.co.qsnext.employeemanagement.timesheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "timesheet_entries")
public class TimesheetEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timesheet_id", nullable = false)
    private UUID timesheetId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(
            name = "hours_worked",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal hoursWorked;

    @Column(name = "description", length = 500)
    private String description;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected TimesheetEntry() {
        // Required by JPA
    }

    public TimesheetEntry(
            UUID timesheetId,
            LocalDate workDate,
            BigDecimal hoursWorked,
            String description
    ) {
        this.timesheetId = timesheetId;
        this.workDate = workDate;
        this.hoursWorked = hoursWorked;
        this.description = description;
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

    public UUID getTimesheetId() {
        return timesheetId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public String getDescription() {
        return description;
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

    public void update(
            BigDecimal hoursWorked,
            String description
    ) {
        this.hoursWorked = hoursWorked;
        this.description = description;
    }
}