package za.co.qsnext.employeemanagement.onboarding.dto;

import za.co.qsnext.employeemanagement.onboarding.OnboardingTemplate;

import java.util.List;
import java.util.UUID;

public record OnboardingTemplateResponse(
        UUID id,
        String name,
        String description,
        List<OnboardingTemplateTaskResponse> tasks
) {

    public static OnboardingTemplateResponse from(
            OnboardingTemplate template,
            List<OnboardingTemplateTaskResponse> tasks
    ) {
        return new OnboardingTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                tasks
        );
    }
}
