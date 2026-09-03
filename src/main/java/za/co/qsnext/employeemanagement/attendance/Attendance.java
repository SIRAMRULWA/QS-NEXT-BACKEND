package za.co.qsnext.employeemanagement.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "clock_in")
    private OffsetDateTime clockIn;

    @Column(name = "clock_out")
    private OffsetDateTime clockOut;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "notes", length = 500)
    private String notes;

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

    protected Attendance() {
        // Required by JPA
    }

    public Attendance(
            UUID employeeId,
            LocalDate attendanceDate
    ) {
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.status = "PRESENT";
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

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public OffsetDateTime getClockIn() {
        return clockIn;
    }

    public OffsetDateTime getClockOut() {
        return clockOut;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
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

    public void clockIn() {
        this.clockIn = OffsetDateTime.now();
        this.status = "PRESENT";
    }

    public void clockOut() {
        this.clockOut = OffsetDateTime.now();
    }

    public void markAbsent() {
        this.status = "ABSENT";
    }

    public void markLate() {
        this.status = "LATE";
    }

    public void markHalfDay() {
        this.status = "HALF_DAY";
    }

    public void markOnLeave() {
        this.status = "ON_LEAVE";
    }

    public void markRemote() {
        this.status = "REMOTE";
    }

    public void updateNotes(String notes) {
        this.notes = notes;
    }
}