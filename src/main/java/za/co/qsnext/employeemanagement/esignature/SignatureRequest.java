package za.co.qsnext.employeemanagement.esignature;

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
 * A request for one or more signers to sign a specific {@code Document}
 * version. This module deliberately does not claim to produce a legally
 * binding signature on its own - see {@link ESignatureProvider}, the
 * extension point a real external e-signature vendor would plug into.
 */
@Entity
@Table(name = "signature_requests")
public class SignatureRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SignatureRequest() {
        // Required by JPA
    }

    public SignatureRequest(UUID documentId, String title, UUID requestedBy, OffsetDateTime expiresAt) {
        this.documentId = documentId;
        this.title = title;
        this.requestedBy = requestedBy;
        this.status = STATUS_PENDING;
        this.expiresAt = expiresAt;
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

    public UUID getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
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

    public void complete() {
        this.status = STATUS_COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void decline() {
        this.status = STATUS_DECLINED;
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
    }

    public void expire() {
        this.status = STATUS_EXPIRED;
    }
}
