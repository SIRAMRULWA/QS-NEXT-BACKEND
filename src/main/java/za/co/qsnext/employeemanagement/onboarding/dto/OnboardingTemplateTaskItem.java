package za.co.qsnext.employeemanagement.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One task definition within a {@code CreateOnboardingTemplateRequest}.
 */
public record OnboardingTemplateTaskItem(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Assignee role is required")
        @Pattern(
                regexp = "HR|MANAGER|EMPLOYEE",
                message = "Assignee role must be one of HR, MANAGER, EMPLOYEE"
        )
        String assigneeRole,

        int sortOrder
) {
}
