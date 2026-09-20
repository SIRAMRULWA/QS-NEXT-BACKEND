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

@Entity
@Table(name = "interviews")
public class Interview {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "interviewer_user_id", nullable = false, updatable = false)
    private UUID interviewerUserId;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Interview() {
        // Required by JPA
    }

    public Interview(
            UUID applicationId,
            UUID interviewerUserId,
            OffsetDateTime scheduledAt,
            int durationMinutes,
            String location
    ) {
        this.applicationId = applicationId;
        this.interviewerUserId = interviewerUserId;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.location = location;
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

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getInterviewerUserId() {
        return interviewerUserId;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getLocation() {
        return location;
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

    public boolean isScheduled() {
        return STATUS_SCHEDULED.equals(status);
    }

    public void complete() {
        this.status = STATUS_COMPLETED;
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
    }
}
