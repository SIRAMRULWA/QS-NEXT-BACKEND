package za.co.qsnext.employeemanagement.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A configurable pay cycle definition (e.g. "2026-01 Monthly"). At most
 * one {@link PayrollRun} exists per period - see the unique constraint
 * on {@code payroll_runs.pay_period_id}.
 */
@Entity
@Table(name = "pay_periods")
public class PayPeriod {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PayPeriod() {
        // Required by JPA
    }

    public PayPeriod(String name, LocalDate startDate, LocalDate endDate, LocalDate payDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
