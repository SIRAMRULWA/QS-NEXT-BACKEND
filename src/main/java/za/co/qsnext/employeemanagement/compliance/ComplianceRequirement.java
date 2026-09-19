package za.co.qsnext.employeemanagement.compliance;

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

/**
 * A configurable compliance requirement template (e.g. "ID Document",
 * "POPIA Policy Acknowledgement", "Fire Safety Training"). This is
 * deliberately generic rather than hardcoding specific South African (or
 * any other jurisdiction's) legal rules - {@link #category} and
 * {@link #validityPeriodDays} are configuration, not legal conclusions
 * baked into code.
 */
@Entity
@Table(name = "compliance_requirements")
public class ComplianceRequirement {

    public static final String CATEGORY_DOCUMENT = "DOCUMENT";
    public static final String CATEGORY_POLICY_ACKNOWLEDGEMENT = "POLICY_ACKNOWLEDGEMENT";
    public static final String CATEGORY_TRAINING = "TRAINING";
    public static final String CATEGORY_OTHER = "OTHER";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory;

    /**
     * How long a completed record remains valid, in days, before it is
     * due for renewal. Null means the requirement never expires once met.
     */
    @Column(name = "validity_period_days")
    private Integer validityPeriodDays;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ComplianceRequirement() {
        // Required by JPA
    }

    public ComplianceRequirement(
            String name,
            String description,
            String category,
            boolean mandatory,
            Integer validityPeriodDays
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.mandatory = mandatory;
        this.validityPeriodDays = validityPeriodDays;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public Integer getValidityPeriodDays() {
        return validityPeriodDays;
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

    public void deactivate() {
        this.active = false;
    }
}
