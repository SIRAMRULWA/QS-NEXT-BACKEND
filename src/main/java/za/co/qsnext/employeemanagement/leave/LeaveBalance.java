package za.co.qsnext.employeemanagement.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leave_balances_employee_type_year",
                        columnNames = {
                                "employee_id",
                                "leave_type",
                                "leave_year"
                        }
                )
        }
)
public class LeaveBalance {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType;

    @Column(name = "leave_year", nullable = false)
    private Integer leaveYear;

    @Column(
            name = "allocated_days",
            nullable = false,
            precision = 6,
            scale = 2
    )
    private BigDecimal allocatedDays = BigDecimal.ZERO;

    @Column(
            name = "used_days",
            nullable = false,
            precision = 6,
            scale = 2
    )
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LeaveBalance() {
        // Required by JPA
    }

    public LeaveBalance(
            UUID employeeId,
            String leaveType,
            Integer leaveYear,
            BigDecimal allocatedDays
    ) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.leaveYear = leaveYear;
        this.allocatedDays = allocatedDays;
        this.usedDays = BigDecimal.ZERO;
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

    public String getLeaveType() {
        return leaveType;
    }

    public Integer getLeaveYear() {
        return leaveYear;
    }

    public BigDecimal getAllocatedDays() {
        return allocatedDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public BigDecimal getRemainingDays() {
        return allocatedDays.subtract(usedDays);
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

    public void addUsedDays(BigDecimal days) {
        this.usedDays = this.usedDays.add(days);
    }

    public void allocateAdditionalDays(BigDecimal days) {
        this.allocatedDays = this.allocatedDays.add(days);
    }
}