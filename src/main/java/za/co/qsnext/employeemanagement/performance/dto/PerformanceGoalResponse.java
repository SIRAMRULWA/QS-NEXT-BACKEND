package za.co.qsnext.employeemanagement.performance.dto;

import za.co.qsnext.employeemanagement.performance.PerformanceGoal;

import java.time.LocalDate;
import java.util.UUID;

public record PerformanceGoalResponse(
        UUID id,
        UUID cycleId,
        UUID employeeId,
        String title,
        String description,
        LocalDate targetDate,
        String status
) {

    public static PerformanceGoalResponse from(PerformanceGoal goal) {
        return new PerformanceGoalResponse(
                goal.getId(), goal.getCycleId(), goal.getEmployeeId(), goal.getTitle(),
                goal.getDescription(), goal.getTargetDate(), goal.getStatus()
        );
    }
}
