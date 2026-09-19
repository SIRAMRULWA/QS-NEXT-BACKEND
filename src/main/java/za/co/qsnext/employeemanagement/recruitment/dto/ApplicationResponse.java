package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.Application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID candidateId,
        UUID jobPostingId,
        String status,
        String rejectionReason,
        OffsetDateTime appliedAt
) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(), application.getCandidateId(), application.getJobPostingId(),
                application.getStatus(), application.getRejectionReason(), application.getAppliedAt()
        );
    }
}
