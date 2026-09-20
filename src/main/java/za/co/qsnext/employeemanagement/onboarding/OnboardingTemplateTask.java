package za.co.qsnext.employeemanagement.onboarding;

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
 * One task in an {@link OnboardingTemplate}. When a workflow is started
 * from the template, each of these is copied into a fresh
 * {@link OnboardingTask} - the template is a reusable blueprint, the
 * workflow's tasks are the actual per-employee, per-attempt instances.
 */
@Entity
@Table(name = "onboarding_template_tasks")
public class OnboardingTemplateTask {

    public static final String ROLE_HR = "HR";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "assignee_role", nullable = false, length = 20)
    private String assigneeRole;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OnboardingTemplateTask() {
        // Required by JPA
    }

    public OnboardingTemplateTask(
            UUID templateId,
            String title,
            String description,
            String assigneeRole,
            int sortOrder
    ) {
        this.templateId = templateId;
        this.title = title;
        this.description = description;
        this.assigneeRole = assigneeRole;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssigneeRole() {
        return assigneeRole;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
