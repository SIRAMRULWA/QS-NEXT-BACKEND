package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.Interview;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID applicationId,
        UUID interviewerUserId,
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String location,
        String status
) {

    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(), interview.getApplicationId(), interview.getInterviewerUserId(),
                interview.getScheduledAt(), interview.getDurationMinutes(), interview.getLocation(),
                interview.getStatus()
        );
    }
}
