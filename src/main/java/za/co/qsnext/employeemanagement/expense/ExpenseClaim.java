package za.co.qsnext.employeemanagement.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One employee's expense claim: DRAFT -&gt; SUBMITTED -&gt; APPROVED/REJECTED,
 * and, once approved, eventually REIMBURSED - mirroring the
 * draft/submit/approve/reject shape already established by
 * {@code Timesheet} and {@code LeaveRequest}.
 */
@Entity
@Table(name = "expense_claims")
public class ExpenseClaim {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REIMBURSED = "REIMBURSED";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "expense_date", nullable = false, updatable = false)
    private LocalDate expenseDate;

    @Column(name = "receipt_document_id")
    private UUID receiptDocumentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reimbursed_at")
    private OffsetDateTime reimbursedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ExpenseClaim() {
        // Required by JPA
    }

    public ExpenseClaim(
            UUID employeeId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate expenseDate,
            UUID receiptDocumentId
    ) {
        this.employeeId = employeeId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.expenseDate = expenseDate;
        this.receiptDocumentId = receiptDocumentId;
        this.status = STATUS_DRAFT;
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public UUID getReceiptDocumentId() {
        return receiptDocumentId;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public UUID getRejectedBy() {
        return rejectedBy;
    }

    public OffsetDateTime getRejectedAt() {
        return rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public OffsetDateTime getReimbursedAt() {
        return reimbursedAt;
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

    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public void submit() {
        this.status = STATUS_SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
    }

    public void approve(UUID approvedBy) {
        this.status = STATUS_APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = OffsetDateTime.now();
    }

    public void reject(UUID rejectedBy, String reason) {
        this.status = STATUS_REJECTED;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = OffsetDateTime.now();
        this.rejectionReason = reason;
    }

    public void markReimbursed() {
        this.status = STATUS_REIMBURSED;
        this.reimbursedAt = OffsetDateTime.now();
    }
}
