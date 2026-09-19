package za.co.qsnext.employeemanagement.compliance;

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
 * One employee's instance of a {@link ComplianceRequirement}: assigned,
 * then completed (optionally with a {@code Document} attached as audit
 * evidence), and eventually expiring if the requirement has a validity
 * period.
 */
@Entity
@Table(name = "compliance_records")
public class ComplianceRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "requirement_id", nullable = false, updatable = false)
    private UUID requirementId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "evidence_document_id")
    private UUID evidenceDocumentId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ComplianceRecord() {
        // Required by JPA
    }

    public ComplianceRecord(UUID employeeId, UUID requirementId) {
        this.employeeId = employeeId;
        this.requirementId = requirementId;
        this.status = STATUS_PENDING;
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

    public UUID getRequirementId() {
        return requirementId;
    }

    public String getStatus() {
        return status;
    }

    public UUID getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public void complete(UUID evidenceDocumentId, String notes, UUID reviewedBy, OffsetDateTime expiresAt) {
        this.status = STATUS_COMPLETED;
        this.evidenceDocumentId = evidenceDocumentId;
        this.notes = notes;
        this.reviewedBy = reviewedBy;
        this.completedAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
    }
}
