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
@Table(name = "job_postings")
public class JobPosting {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "requisition_id", nullable = false, updatable = false)
    private UUID requisitionId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "employment_type", nullable = false, length = 20)
    private String employmentType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected JobPosting() {
        // Required by JPA
    }

    public JobPosting(
            UUID requisitionId,
            String title,
            String description,
            String location,
            String employmentType
    ) {
        this.requisitionId = requisitionId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.employmentType = employmentType;
        this.status = STATUS_OPEN;
        this.publishedAt = OffsetDateTime.now();
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

    public UUID getRequisitionId() {
        return requisitionId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
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

    public void close() {
        this.status = STATUS_CLOSED;
        this.closedAt = OffsetDateTime.now();
    }
}
