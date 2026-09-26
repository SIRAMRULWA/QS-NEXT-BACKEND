package za.co.qsnext.employeemanagement.careers.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MyApplicationResponse(
        UUID id,
        UUID jobPostingId,
        String jobTitle,
        String location,
        String status,
        OffsetDateTime appliedAt,
        OffsetDateTime updatedAt,
        List<MyInterviewResponse> interviews
) {
}
