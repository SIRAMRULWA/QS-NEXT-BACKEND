package za.co.qsnext.employeemanagement.analytics.dto;

public record EngagementAnalyticsResponse(
        long totalRecognitions,
        long totalPoints,
        long distinctGivers,
        long distinctRecipients
) {
}
