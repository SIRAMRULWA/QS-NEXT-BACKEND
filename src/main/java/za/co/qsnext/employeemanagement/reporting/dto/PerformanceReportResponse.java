package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.performance.PerformanceReview;

import java.util.List;
import java.util.UUID;

public record PerformanceReportResponse(
        UUID employeeId,
        int totalReviews,
        int completedReviews,
        int inProgressReviews,
        int draftReviews,
        Double averageManagerRating,
        List<ReviewSummary> reviews
) {

    public static PerformanceReportResponse from(UUID employeeId, List<PerformanceReview> reviews) {

        int completed = 0;
        int inProgress = 0;
        int draft = 0;
        int ratingSum = 0;
        int ratingCount = 0;

        for (PerformanceReview review : reviews) {
            switch (review.getStatus()) {
                case PerformanceReview.STATUS_COMPLETED -> completed++;
                case PerformanceReview.STATUS_IN_PROGRESS -> inProgress++;
                case PerformanceReview.STATUS_DRAFT -> draft++;
                default -> {
                    // Database CHECK constraint prevents unknown statuses.
                }
            }

            if (review.getManagerRating() != null) {
                ratingSum += review.getManagerRating();
                ratingCount++;
            }
        }

        Double averageRating = ratingCount == 0 ? null : (double) ratingSum / ratingCount;

        return new PerformanceReportResponse(
                employeeId, reviews.size(), completed, inProgress, draft, averageRating,
                reviews.stream().map(ReviewSummary::from).toList());
    }

    public record ReviewSummary(
            UUID id,
            UUID cycleId,
            String status,
            Integer selfRating,
            Integer managerRating
    ) {

        public static ReviewSummary from(PerformanceReview review) {
            return new ReviewSummary(
                    review.getId(), review.getCycleId(), review.getStatus(),
                    review.getSelfRating(), review.getManagerRating());
        }
    }
}
