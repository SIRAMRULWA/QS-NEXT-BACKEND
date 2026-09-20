package za.co.qsnext.employeemanagement.learning;

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

@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment {

    public static final String STATUS_ENROLLED = "ENROLLED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "certificate_issued_at")
    private OffsetDateTime certificateIssuedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CourseEnrollment() {
        // Required by JPA
    }

    public CourseEnrollment(UUID employeeId, UUID courseId) {
        this.employeeId = employeeId;
        this.courseId = courseId;
        this.status = STATUS_ENROLLED;
        this.progressPercent = 0;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        enrolledAt = now;
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

    public UUID getCourseId() {
        return courseId;
    }

    public String getStatus() {
        return status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getCertificateIssuedAt() {
        return certificateIssuedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public void updateProgress(int progressPercent) {
        this.progressPercent = progressPercent;
        this.status = progressPercent >= 100 ? STATUS_COMPLETED : STATUS_IN_PROGRESS;

        if (isCompleted() && completedAt == null) {
            markCompleted();
        }
    }

    public void markCompleted() {
        this.status = STATUS_COMPLETED;
        this.progressPercent = 100;
        this.completedAt = OffsetDateTime.now();
        this.certificateIssuedAt = OffsetDateTime.now();
    }
}
