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
import java.util.UUID;

/**
 * One employee's payroll configuration. Deliberately does not store a
 * full bank account number - {@link #bankAccountReference} is an opaque
 * reference (e.g. a masked "****1234" or an external payment provider's
 * account token) resolved to real banking details outside this system,
 * consistent with the project's secrets policy of never holding
 * sensitive financial credentials in this database.
 */
@Entity
@Table(name = "employee_payroll_profiles")
public class EmployeePayrollProfile {

    public static final String FREQUENCY_MONTHLY = "MONTHLY";
    public static final String FREQUENCY_BIWEEKLY = "BIWEEKLY";
    public static final String FREQUENCY_WEEKLY = "WEEKLY";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true, updatable = false)
    private UUID employeeId;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "pay_frequency", nullable = false, length = 20)
    private String payFrequency;

    /**
     * Expected hours for one pay period - hours worked beyond this
     * (from APPROVED timesheets) generate an OVERTIME earning line at
     * {@link #overtimeHourlyRate}. Null means overtime is not tracked
     * for this employee (e.g. a salaried role).
     */
    @Column(name = "standard_hours_per_period", precision = 6, scale = 2)
    private BigDecimal standardHoursPerPeriod;

    @Column(name = "overtime_hourly_rate", precision = 10, scale = 2)
    private BigDecimal overtimeHourlyRate;

    @Column(name = "bank_account_reference", length = 100)
    private String bankAccountReference;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EmployeePayrollProfile() {
        // Required by JPA
    }

    public EmployeePayrollProfile(
            UUID employeeId,
            BigDecimal baseSalary,
            String payFrequency,
            BigDecimal standardHoursPerPeriod,
            BigDecimal overtimeHourlyRate,
            String bankAccountReference,
            String taxNumber
    ) {
        this.employeeId = employeeId;
        this.baseSalary = baseSalary;
        this.payFrequency = payFrequency;
        this.standardHoursPerPeriod = standardHoursPerPeriod;
        this.overtimeHourlyRate = overtimeHourlyRate;
        this.bankAccountReference = bankAccountReference;
        this.taxNumber = taxNumber;
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

    public UUID getEmployeeId() {
        return employeeId;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public String getPayFrequency() {
        return payFrequency;
    }

    public BigDecimal getStandardHoursPerPeriod() {
        return standardHoursPerPeriod;
    }

    public BigDecimal getOvertimeHourlyRate() {
        return overtimeHourlyRate;
    }

    public String getBankAccountReference() {
        return bankAccountReference;
    }

    public String getTaxNumber() {
        return taxNumber;
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

    public void update(
            BigDecimal baseSalary,
            String payFrequency,
            BigDecimal standardHoursPerPeriod,
            BigDecimal overtimeHourlyRate,
            String bankAccountReference,
            String taxNumber
    ) {
        this.baseSalary = baseSalary;
        this.payFrequency = payFrequency;
        this.standardHoursPerPeriod = standardHoursPerPeriod;
        this.overtimeHourlyRate = overtimeHourlyRate;
        this.bankAccountReference = bankAccountReference;
        this.taxNumber = taxNumber;
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }
}
