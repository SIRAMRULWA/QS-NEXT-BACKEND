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
 * An internal request/approval to hire for a role. {@link JobPosting}s
 * (the external-facing listings) reference one of these.
 */
@Entity
@Table(name = "job_requisitions")
public class JobRequisition {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ON_HOLD = "ON_HOLD";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "number_of_openings", nullable = false)
    private int numberOfOpenings;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected JobRequisition() {
        // Required by JPA
    }

    public JobRequisition(
            String title,
            UUID departmentId,
            String description,
            int numberOfOpenings,
            UUID requestedBy
    ) {
        this.title = title;
        this.departmentId = departmentId;
        this.description = description;
        this.numberOfOpenings = numberOfOpenings;
        this.requestedBy = requestedBy;
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

    public String getTitle() {
        return title;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public String getDescription() {
        return description;
    }

    public int getNumberOfOpenings() {
        return numberOfOpenings;
    }

    public String getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    public void putOnHold() {
        this.status = STATUS_ON_HOLD;
    }

    public void reopen() {
        this.status = STATUS_OPEN;
    }

    public void close() {
        this.status = STATUS_CLOSED;
    }
}
