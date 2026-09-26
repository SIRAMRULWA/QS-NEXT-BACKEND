package za.co.qsnext.employeemanagement.careers.dto;

import za.co.qsnext.employeemanagement.recruitment.Interview;

import java.time.OffsetDateTime;

/**
 * An interview as the applicant sees it: when and where, not who is
 * interviewing or their feedback.
 */
public record MyInterviewResponse(
        OffsetDateTime scheduledAt,
        int durationMinutes,
        String location,
        String status
) {

    public static MyInterviewResponse from(Interview interview) {
        return new MyInterviewResponse(
                interview.getScheduledAt(),
                interview.getDurationMinutes(),
                interview.getLocation(),
                interview.getStatus()
        );
    }
}
