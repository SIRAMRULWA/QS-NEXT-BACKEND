package za.co.qsnext.employeemanagement.learning;

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

@Entity
@Table(name = "employee_skills")
public class EmployeeSkill {

    public static final String LEVEL_BEGINNER = "BEGINNER";
    public static final String LEVEL_INTERMEDIATE = "INTERMEDIATE";
    public static final String LEVEL_ADVANCED = "ADVANCED";
    public static final String LEVEL_EXPERT = "EXPERT";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "skill_id", nullable = false, updatable = false)
    private UUID skillId;

    @Column(name = "proficiency_level", nullable = false, length = 20)
    private String proficiencyLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EmployeeSkill() {
        // Required by JPA
    }

    public EmployeeSkill(UUID employeeId, UUID skillId, String proficiencyLevel) {
        this.employeeId = employeeId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
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

    public UUID getSkillId() {
        return skillId;
    }

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProficiency(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}
