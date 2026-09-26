package za.co.qsnext.employeemanagement.careers.dto;

import za.co.qsnext.employeemanagement.recruitment.JobPosting;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A published job as an applicant sees it: no requisition, budget or
 * hiring-manager details.
 */
public record JobOpeningResponse(
        UUID id,
        String title,
        String description,
        String location,
        String employmentType,
        OffsetDateTime publishedAt
) {

    public static JobOpeningResponse from(JobPosting posting) {
        return new JobOpeningResponse(
                posting.getId(),
                posting.getTitle(),
                posting.getDescription(),
                posting.getLocation(),
                posting.getEmploymentType(),
                posting.getPublishedAt()
        );
    }
}
