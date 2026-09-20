package za.co.qsnext.employeemanagement.onboarding.dto;

import za.co.qsnext.employeemanagement.onboarding.OnboardingTask;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OnboardingTaskResponse(
        UUID id,
        UUID workflowId,
        String title,
        String description,
        String assigneeRole,
        String status,
        int sortOrder,
        OffsetDateTime completedAt,
        UUID completedBy
) {

    public static OnboardingTaskResponse from(OnboardingTask task) {
        return new OnboardingTaskResponse(
                task.getId(),
                task.getWorkflowId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssigneeRole(),
                task.getStatus(),
                task.getSortOrder(),
                task.getCompletedAt(),
                task.getCompletedBy()
        );
    }
}
