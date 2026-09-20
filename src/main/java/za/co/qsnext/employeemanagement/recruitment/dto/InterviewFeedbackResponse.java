package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.InterviewFeedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewFeedbackResponse(
        UUID id,
        UUID interviewId,
        UUID interviewerUserId,
        Integer rating,
        String comments,
        String recommendation,
        OffsetDateTime submittedAt
) {

    public static InterviewFeedbackResponse from(InterviewFeedback feedback) {
        return new InterviewFeedbackResponse(
                feedback.getId(), feedback.getInterviewId(), feedback.getInterviewerUserId(),
                feedback.getRating(), feedback.getComments(), feedback.getRecommendation(),
                feedback.getSubmittedAt()
        );
    }
}
