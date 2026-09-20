package za.co.qsnext.employeemanagement.performance.dto;

import za.co.qsnext.employeemanagement.performance.DevelopmentPlan;

import java.time.LocalDate;
import java.util.UUID;

public record DevelopmentPlanResponse(
        UUID id,
        UUID employeeId,
        UUID reviewId,
        String description,
        UUID recommendedCourseId,
        LocalDate targetDate,
        String status
) {

    public static DevelopmentPlanResponse from(DevelopmentPlan plan) {
        return new DevelopmentPlanResponse(
                plan.getId(), plan.getEmployeeId(), plan.getReviewId(), plan.getDescription(),
                plan.getRecommendedCourseId(), plan.getTargetDate(), plan.getStatus()
        );
    }
}
