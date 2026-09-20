package za.co.qsnext.employeemanagement.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_line_items")
public class PayrollLineItem {

    public static final String TYPE_EARNING = "EARNING";
    public static final String TYPE_DEDUCTION = "DEDUCTION";
    public static final String TYPE_EMPLOYER_CONTRIBUTION = "EMPLOYER_CONTRIBUTION";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payroll_run_entry_id", nullable = false, updatable = false)
    private UUID payrollRunEntryId;

    @Column(name = "type", nullable = false, length = 30, updatable = false)
    private String type;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "description", length = 500, updatable = false)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PayrollLineItem() {
        // Required by JPA
    }

    public PayrollLineItem(UUID payrollRunEntryId, String type, String code, String description, BigDecimal amount) {
        this.payrollRunEntryId = payrollRunEntryId;
        this.type = type;
        this.code = code;
        this.description = description;
        this.amount = amount;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPayrollRunEntryId() {
        return payrollRunEntryId;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
