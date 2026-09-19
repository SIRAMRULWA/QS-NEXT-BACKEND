package za.co.qsnext.employeemanagement.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Metadata for one uploaded file. The bytes themselves live behind
 * {@link DocumentStorageService}, identified here only by
 * {@code storageKey} - this row (and its version history, grouped by
 * {@code documentFamilyId}) is the source of truth for access control,
 * expiry and audit, not for the file content itself.
 */
@Entity
@Table(name = "documents")
public class Document {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "document_family_id", nullable = false, updatable = false)
    private UUID documentFamilyId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "storage_key", nullable = false, length = 500, updatable = false)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255, updatable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false, updatable = false)
    private long fileSizeBytes;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Document() {
        // Required by JPA
    }

    public Document(
            UUID employeeId,
            String category,
            String title,
            String description,
            UUID documentFamilyId,
            int version,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            LocalDate expiryDate,
            UUID uploadedBy
    ) {
        this.employeeId = employeeId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.documentFamilyId = documentFamilyId;
        this.version = version;
        this.status = STATUS_ACTIVE;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.expiryDate = expiryDate;
        this.uploadedBy = uploadedBy;
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

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getDocumentFamilyId() {
        return documentFamilyId;
    }

    public int getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void supersede() {
        this.status = STATUS_SUPERSEDED;
    }

    public void archive() {
        this.status = STATUS_ARCHIVED;
    }
}
