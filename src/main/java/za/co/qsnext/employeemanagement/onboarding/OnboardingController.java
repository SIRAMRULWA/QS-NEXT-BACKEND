package za.co.qsnext.employeemanagement.onboarding;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.onboarding.dto.CreateOnboardingTemplateRequest;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTaskResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingWorkflowResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PreAuthorize("hasAuthority('ONBOARDING_MANAGE')")
    @PostMapping("/templates")
    public ResponseEntity<OnboardingTemplateResponse> createTemplate(
            @Valid @RequestBody CreateOnboardingTemplateRequest request
    ) {
        OnboardingTemplateResponse response = onboardingService.createTemplate(
                request.name(), request.description(), request.tasks()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('ONBOARDING_MANAGE')")
    @GetMapping("/templates")
    public ResponseEntity<List<OnboardingTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(onboardingService.getAllTemplates());
    }

    @PreAuthorize("hasAuthority('ONBOARDING_MANAGE')")
    @PostMapping("/workflows")
    public ResponseEntity<OnboardingWorkflowResponse> startWorkflow(
            @RequestParam UUID employeeId,
            @RequestParam UUID templateId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                onboardingService.startWorkflow(employeeId, templateId)
        );
    }

    @PreAuthorize("hasAuthority('ONBOARDING_MANAGE')")
    @GetMapping("/employees/{employeeId}/workflows")
    public ResponseEntity<List<OnboardingWorkflowResponse>> getWorkflowsForEmployee(
            @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(onboardingService.getWorkflowsForEmployee(employeeId));
    }

    @PreAuthorize("hasAuthority('ONBOARDING_MANAGE')")
    @GetMapping("/workflows/{workflowId}/tasks")
    public ResponseEntity<List<OnboardingTaskResponse>> getWorkflowTasks(
            @PathVariable UUID workflowId
    ) {
        return ResponseEntity.ok(onboardingService.getWorkflowTasks(workflowId));
    }

    @PreAuthorize("hasAuthority('ONBOARDING_TASK_READ')")
    @GetMapping("/my-tasks")
    public ResponseEntity<List<OnboardingTaskResponse>> getOwnTasks(
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(onboardingService.getOwnTasks(userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('ONBOARDING_TASK_READ')")
    @PatchMapping("/tasks/{taskId}/complete")
    public ResponseEntity<OnboardingTaskResponse> completeTask(
            Authentication authentication,
            @PathVariable UUID taskId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        boolean canManageOnboarding = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ONBOARDING_MANAGE".equals(authority.getAuthority()));

        return ResponseEntity.ok(
                onboardingService.completeTask(taskId, userDetails.getUserId(), canManageOnboarding)
        );
    }
}
