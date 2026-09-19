package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleInterviewRequest(

        @NotNull(message = "Application ID is required")
        UUID applicationId,

        @NotNull(message = "Interviewer user ID is required")
        UUID interviewerUserId,

        @NotNull(message = "Scheduled time is required")
        @Future(message = "Scheduled time must be in the future")
        OffsetDateTime scheduledAt,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        int durationMinutes,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location
) {
}
