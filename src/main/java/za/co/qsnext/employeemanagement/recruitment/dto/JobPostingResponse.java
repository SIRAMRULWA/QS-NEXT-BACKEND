package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.JobPosting;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobPostingResponse(
        UUID id,
        UUID requisitionId,
        String title,
        String description,
        String location,
        String employmentType,
        String status,
        OffsetDateTime publishedAt,
        OffsetDateTime closedAt
) {

    public static JobPostingResponse from(JobPosting posting) {
        return new JobPostingResponse(
                posting.getId(), posting.getRequisitionId(), posting.getTitle(), posting.getDescription(),
                posting.getLocation(), posting.getEmploymentType(), posting.getStatus(),
                posting.getPublishedAt(), posting.getClosedAt()
        );
    }
}
