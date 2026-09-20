package za.co.qsnext.employeemanagement.onboarding.dto;

import za.co.qsnext.employeemanagement.onboarding.OnboardingTemplateTask;

import java.util.UUID;

public record OnboardingTemplateTaskResponse(
        UUID id,
        String title,
        String description,
        String assigneeRole,
        int sortOrder
) {

    public static OnboardingTemplateTaskResponse from(OnboardingTemplateTask task) {
        return new OnboardingTemplateTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssigneeRole(),
                task.getSortOrder()
        );
    }
}
