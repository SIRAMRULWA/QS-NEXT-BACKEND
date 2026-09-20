package za.co.qsnext.employeemanagement.analytics.dto;

import java.time.LocalDate;

/**
 * {@code terminations} is an approximation, not an exact count - see
 * {@code AnalyticsService#getTurnoverAnalytics} for why, and
 * {@link #terminationMethodology()} for a caller-facing explanation.
 */
public record TurnoverAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long hires,
        long terminations,
        String terminationMethodology
) {
}
