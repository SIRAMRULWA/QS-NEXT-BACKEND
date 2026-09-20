package za.co.qsnext.employeemanagement.ai;

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
 * A durable record of one AI-generated response, kept so every AI
 * capability's output is reviewable by an authorized human after the
 * fact - per the project's rule that AI recommendations must remain
 * reviewable, never silently acted on. Nothing in this module writes
 * to any other entity: every capability in {@code AiService} only ever
 * reads domain data and produces one of these rows.
 */
@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {

    public static final String TYPE_HR_ASSISTANT = "HR_ASSISTANT";
    public static final String TYPE_DOCUMENT_SUMMARY = "DOCUMENT_SUMMARY";
    public static final String TYPE_JOB_DESCRIPTION = "JOB_DESCRIPTION";
    public static final String TYPE_CANDIDATE_MATCH = "CANDIDATE_MATCH";
    public static final String TYPE_SKILLS_RECOMMENDATION = "SKILLS_RECOMMENDATION";
    public static final String TYPE_LEARNING_RECOMMENDATION = "LEARNING_RECOMMENDATION";
    public static final String TYPE_ANALYTICS_EXPLANATION = "ANALYTICS_EXPLANATION";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 40, updatable = false)
    private String type;

    @Column(name = "subject_type", length = 40, updatable = false)
    private String subjectType;

    @Column(name = "subject_id", updatable = false)
    private UUID subjectId;

    @Column(name = "requested_by_user_id", nullable = false, updatable = false)
    private UUID requestedByUserId;

    @Column(name = "provider_name", nullable = false, length = 50, updatable = false)
    private String providerName;

    @Column(name = "prompt", nullable = false, updatable = false)
    private String prompt;

    @Column(name = "response_text", nullable = false, updatable = false)
    private String responseText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiSuggestion() {
        // Required by JPA
    }

    public AiSuggestion(
            String type,
            String subjectType,
            UUID subjectId,
            UUID requestedByUserId,
            String providerName,
            String prompt,
            String responseText
    ) {
        this.type = type;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.requestedByUserId = requestedByUserId;
        this.providerName = providerName;
        this.prompt = prompt;
        this.responseText = responseText;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponseText() {
        return responseText;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
