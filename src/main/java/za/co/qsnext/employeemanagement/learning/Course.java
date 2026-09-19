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

/**
 * A course employees can enrol in. {@link #linkedComplianceRequirementId},
 * when set, ties completion back to a {@code ComplianceRequirement} -
 * see {@code CourseEnrollmentService#complete} for the "compliance
 * training integration" this satisfies.
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory;

    @Column(name = "linked_compliance_requirement_id")
    private UUID linkedComplianceRequirementId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Course() {
        // Required by JPA
    }

    public Course(
            String title,
            String description,
            String category,
            Integer durationMinutes,
            boolean mandatory,
            UUID linkedComplianceRequirementId
    ) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.durationMinutes = durationMinutes;
        this.mandatory = mandatory;
        this.linkedComplianceRequirementId = linkedComplianceRequirementId;
        this.active = true;
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

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public UUID getLinkedComplianceRequirementId() {
        return linkedComplianceRequirementId;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
