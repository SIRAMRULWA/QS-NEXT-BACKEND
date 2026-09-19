package za.co.qsnext.employeemanagement.payroll;

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
@Table(name = "payroll_runs")
public class PayrollRun {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PAID = "PAID";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pay_period_id", nullable = false, unique = true, updatable = false)
    private UUID payPeriodId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "run_by_user_id", nullable = false, updatable = false)
    private UUID runByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PayrollRun() {
        // Required by JPA
    }

    public PayrollRun(UUID payPeriodId, UUID runByUserId) {
        this.payPeriodId = payPeriodId;
        this.runByUserId = runByUserId;
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

    public UUID getPayPeriodId() {
        return payPeriodId;
    }

    public String getStatus() {
        return status;
    }

    public UUID getRunByUserId() {
        return runByUserId;
    }

    public UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public void approve(UUID approvedByUserId) {
        this.status = STATUS_APPROVED;
        this.approvedByUserId = approvedByUserId;
        this.approvedAt = OffsetDateTime.now();
    }

    public void markPaid() {
        this.status = STATUS_PAID;
        this.paidAt = OffsetDateTime.now();
    }
}
