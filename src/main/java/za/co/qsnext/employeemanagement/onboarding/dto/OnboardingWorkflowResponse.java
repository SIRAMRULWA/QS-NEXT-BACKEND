package za.co.qsnext.employeemanagement.onboarding.dto;

import za.co.qsnext.employeemanagement.onboarding.OnboardingWorkflow;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OnboardingWorkflowResponse(
        UUID id,
        UUID employeeId,
        UUID templateId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {

    public static OnboardingWorkflowResponse from(OnboardingWorkflow workflow) {
        return new OnboardingWorkflowResponse(
                workflow.getId(),
                workflow.getEmployeeId(),
                workflow.getTemplateId(),
                workflow.getStatus(),
                workflow.getStartedAt(),
                workflow.getCompletedAt()
        );
    }
}
