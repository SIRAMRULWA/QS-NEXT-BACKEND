package za.co.qsnext.employeemanagement.payroll;

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
 * A named, configurable statutory calculation (e.g. "PAYE", "UIF",
 * "SDL" for a South African deployment) - deliberately just a named
 * container for {@link TaxBracket}s, with no rates of any kind baked
 * into code. This system ships with no seeded brackets: the deploying
 * organization must configure current, correct rates from an
 * authoritative source (e.g. SARS) before running real payroll. Nothing
 * here is tax or legal advice.
 */
@Entity
@Table(name = "tax_configurations")
public class TaxConfiguration {

    public static final String LINE_ITEM_TYPE_DEDUCTION = "DEDUCTION";
    public static final String LINE_ITEM_TYPE_EMPLOYER_CONTRIBUTION = "EMPLOYER_CONTRIBUTION";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Whether this generates a DEDUCTION line item (reduces the
     * employee's net pay, e.g. PAYE, an employee's own UIF
     * contribution) or an EMPLOYER_CONTRIBUTION line item (an employer
     * cost tracked for transparency, e.g. SDL, an employer's UIF
     * contribution - does not reduce net pay).
     */
    @Column(name = "line_item_type", nullable = false, length = 30)
    private String lineItemType;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TaxConfiguration() {
        // Required by JPA
    }

    public TaxConfiguration(String name, String description, String lineItemType) {
        this.name = name;
        this.description = description;
        this.lineItemType = lineItemType;
        this.active = true;
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

    public String getDescription() {
        return description;
    }

    public String getLineItemType() {
        return lineItemType;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
