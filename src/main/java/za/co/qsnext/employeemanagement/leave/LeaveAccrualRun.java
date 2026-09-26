package za.co.qsnext.employeemanagement.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records that a policy's accrual for one month has been credited, so it
 * is never credited twice.
 */
@Entity
@Table(name = "leave_accrual_runs")
public class LeaveAccrualRun {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "policy_id", nullable = false, updatable = false)
    private UUID policyId;

    @Column(name = "period", nullable = false, updatable = false, length = 7)
    private String period;

    @Column(name = "employees_credited", nullable = false, updatable = false)
    private int employeesCredited;

    @Column(name = "run_at", nullable = false, updatable = false)
    private OffsetDateTime runAt;

    protected LeaveAccrualRun() {
        // Required by JPA
    }

    public LeaveAccrualRun(UUID policyId, String period, int employeesCredited) {
        this.policyId = policyId;
        this.period = period;
        this.employeesCredited = employeesCredited;
    }

    @PrePersist
    protected void onCreate() {
        runAt = OffsetDateTime.now();
    }
}
