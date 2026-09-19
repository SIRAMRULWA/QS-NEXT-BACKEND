package za.co.qsnext.employeemanagement.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One employee's payslip within a {@link PayrollRun}: totals are
 * recomputed from its {@link PayrollLineItem}s each time a line item is
 * added (see {@code PayrollRunService#recalculate}) - this row's
 * finality is governed entirely by its parent run's status, not an
 * independent status of its own.
 */
@Entity
@Table(name = "payroll_run_entries")
public class PayrollRunEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payroll_run_id", nullable = false, updatable = false)
    private UUID payrollRunId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "total_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarnings;

    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "total_employer_contributions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEmployerContributions;

    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PayrollRunEntry() {
        // Required by JPA
    }

    public PayrollRunEntry(UUID payrollRunId, UUID employeeId) {
        this.payrollRunId = payrollRunId;
        this.employeeId = employeeId;
        this.totalEarnings = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.totalEmployerContributions = BigDecimal.ZERO;
        this.netPay = BigDecimal.ZERO;
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

    public UUID getPayrollRunId() {
        return payrollRunId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public BigDecimal getTotalEmployerContributions() {
        return totalEmployerContributions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Recomputes totals from the entry's current line items. Called
     * after every line item is added so totals never drift out of sync.
     */
    public void recalculate(List<PayrollLineItem> lineItems) {

        this.totalEarnings = sumByType(lineItems, PayrollLineItem.TYPE_EARNING);
        this.totalDeductions = sumByType(lineItems, PayrollLineItem.TYPE_DEDUCTION);
        this.totalEmployerContributions = sumByType(lineItems, PayrollLineItem.TYPE_EMPLOYER_CONTRIBUTION);
        this.netPay = totalEarnings.subtract(totalDeductions);
    }

    private BigDecimal sumByType(List<PayrollLineItem> lineItems, String type) {
        return lineItems.stream()
                .filter(item -> type.equals(item.getType()))
                .map(PayrollLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
