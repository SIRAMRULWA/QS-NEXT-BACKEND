package za.co.qsnext.employeemanagement.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One progressive bracket of a {@link TaxConfiguration}, versioned by
 * {@link #effectiveFrom}/{@link #effectiveTo} so a rate change (e.g. a
 * new SARS tax year) is a new bracket row, not an edit that would
 * silently rewrite how past payroll runs were calculated - see
 * {@code PayrollConfigService#calculateTax}. A flat-rate-with-ceiling
 * calculation (like UIF) is just a single bracket.
 */
@Entity
@Table(name = "tax_brackets")
public class TaxBracket {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tax_configuration_id", nullable = false, updatable = false)
    private UUID taxConfigurationId;

    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal minAmount;

    /**
     * Null means no upper bound (the top bracket).
     */
    @Column(name = "max_amount", precision = 12, scale = 2, updatable = false)
    private BigDecimal maxAmount;

    @Column(name = "rate_percent", nullable = false, precision = 6, scale = 3, updatable = false)
    private BigDecimal ratePercent;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TaxBracket() {
        // Required by JPA
    }

    public TaxBracket(
            UUID taxConfigurationId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal ratePercent,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        this.taxConfigurationId = taxConfigurationId;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.ratePercent = ratePercent;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaxConfigurationId() {
        return taxConfigurationId;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }
}
