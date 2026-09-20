package za.co.qsnext.employeemanagement.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_feedback")
public class InterviewFeedback {

    public static final String RECOMMENDATION_HIRE = "HIRE";
    public static final String RECOMMENDATION_NO_HIRE = "NO_HIRE";
    public static final String RECOMMENDATION_MAYBE = "MAYBE";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "interview_id", nullable = false, unique = true, updatable = false)
    private UUID interviewId;

    @Column(name = "interviewer_user_id", nullable = false, updatable = false)
    private UUID interviewerUserId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "recommendation", nullable = false, length = 20)
    private String recommendation;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    protected InterviewFeedback() {
        // Required by JPA
    }

    public InterviewFeedback(
            UUID interviewId,
            UUID interviewerUserId,
            Integer rating,
            String comments,
            String recommendation
    ) {
        this.interviewId = interviewId;
        this.interviewerUserId = interviewerUserId;
        this.rating = rating;
        this.comments = comments;
        this.recommendation = recommendation;
    }

    @PrePersist
    protected void onCreate() {
        submittedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public UUID getInterviewerUserId() {
        return interviewerUserId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComments() {
        return comments;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
