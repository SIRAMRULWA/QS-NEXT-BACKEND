package za.co.qsnext.employeemanagement.performance.dto;

import za.co.qsnext.employeemanagement.performance.PerformanceReview;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PerformanceReviewResponse(
        UUID id,
        UUID cycleId,
        UUID employeeId,
        UUID reviewerUserId,
        String status,
        Integer selfRating,
        String selfComments,
        OffsetDateTime selfSubmittedAt,
        Integer managerRating,
        String managerComments,
        OffsetDateTime managerSubmittedAt
) {

    public static PerformanceReviewResponse from(PerformanceReview review) {
        return new PerformanceReviewResponse(
                review.getId(), review.getCycleId(), review.getEmployeeId(), review.getReviewerUserId(),
                review.getStatus(), review.getSelfRating(), review.getSelfComments(), review.getSelfSubmittedAt(),
                review.getManagerRating(), review.getManagerComments(), review.getManagerSubmittedAt()
        );
    }
}
