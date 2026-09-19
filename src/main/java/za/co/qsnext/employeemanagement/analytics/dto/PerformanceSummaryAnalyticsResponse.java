package za.co.qsnext.employeemanagement.analytics.dto;

public record PerformanceSummaryAnalyticsResponse(
        long draftReviews,
        long inProgressReviews,
        long completedReviews,
        Double averageCompletedManagerRating
) {
}
