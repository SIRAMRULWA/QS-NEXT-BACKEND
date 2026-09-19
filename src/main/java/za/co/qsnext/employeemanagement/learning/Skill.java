package za.co.qsnext.employeemanagement.learning;

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
 * A named skill in the company-wide catalog (e.g. "Java", "Project
 * Management"). Employees hold skills via {@link EmployeeSkill}.
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Skill() {
        // Required by JPA
    }

    public Skill(String name, String category) {
        this.name = name;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
