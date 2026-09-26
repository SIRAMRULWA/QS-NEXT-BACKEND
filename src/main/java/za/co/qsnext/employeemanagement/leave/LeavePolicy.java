package za.co.qsnext.employeemanagement.leave;

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
 * How one leave type accrues for every active employee: the yearly
 * entitlement, whether it is credited monthly or all at once, and how
 * many unused days carry into the next year.
 */
@Entity
@Table(name = "leave_policies")
public class LeavePolicy {

    public static final String METHOD_MONTHLY = "MONTHLY";
    public static final String METHOD_ANNUAL = "ANNUAL";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "leave_type", nullable = false, unique = true, length = 30)
    private String leaveType;

    @Column(name = "annual_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal annualDays;

    @Column(name = "accrual_method", nullable = false, length = 20)
    private String accrualMethod;

    @Column(name = "carry_over_max_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal carryOverMaxDays;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LeavePolicy() {
        // Required by JPA
    }

    public LeavePolicy(String leaveType) {
        this.leaveType = leaveType;
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

    public void configure(BigDecimal annualDays, String accrualMethod, BigDecimal carryOverMaxDays, boolean active) {
        this.annualDays = annualDays;
        this.accrualMethod = accrualMethod;
        this.carryOverMaxDays = carryOverMaxDays;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public BigDecimal getAnnualDays() {
        return annualDays;
    }

    public String getAccrualMethod() {
        return accrualMethod;
    }

    public BigDecimal getCarryOverMaxDays() {
        return carryOverMaxDays;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
