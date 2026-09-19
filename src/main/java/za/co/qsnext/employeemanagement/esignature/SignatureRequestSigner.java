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

@Entity
@Table(name = "signature_request_signers")
public class SignatureRequestSigner {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SIGNED = "SIGNED";
    public static final String STATUS_DECLINED = "DECLINED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "signature_request_id", nullable = false, updatable = false)
    private UUID signatureRequestId;

    @Column(name = "signer_user_id", nullable = false, updatable = false)
    private UUID signerUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SignatureRequestSigner() {
        // Required by JPA
    }

    public SignatureRequestSigner(UUID signatureRequestId, UUID signerUserId) {
        this.signatureRequestId = signatureRequestId;
        this.signerUserId = signerUserId;
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

    public UUID getSignatureRequestId() {
        return signatureRequestId;
    }

    public UUID getSignerUserId() {
        return signerUserId;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSignedAt() {
        return signedAt;
    }

    public String getDeclineReason() {
        return declineReason;
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

    public void sign() {
        this.status = STATUS_SIGNED;
        this.signedAt = OffsetDateTime.now();
    }

    public void decline(String reason) {
        this.status = STATUS_DECLINED;
        this.declineReason = reason;
    }
}
