package za.co.qsnext.employeemanagement.onboarding;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.OnboardingNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTaskResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateTaskItem;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateTaskResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingWorkflowResponse;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OnboardingService {

    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTemplateTaskRepository templateTaskRepository;
    private final OnboardingWorkflowRepository workflowRepository;
    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;

    public OnboardingService(
            OnboardingTemplateRepository templateRepository,
            OnboardingTemplateTaskRepository templateTaskRepository,
            OnboardingWorkflowRepository workflowRepository,
            OnboardingTaskRepository taskRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher
    ) {
        this.templateRepository = templateRepository;
        this.templateTaskRepository = templateTaskRepository;
        this.workflowRepository = workflowRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional
    public OnboardingTemplateResponse createTemplate(
            String name,
            String description,
            List<OnboardingTemplateTaskItem> taskItems
    ) {
        if (templateRepository.existsByName(name)) {
            throw new DuplicateResourceException("Onboarding template already exists: " + name);
        }

        OnboardingTemplate template = templateRepository.save(
                new OnboardingTemplate(name, description)
        );

        List<OnboardingTemplateTask> tasks = taskItems.stream()
                .map(item -> templateTaskRepository.save(new OnboardingTemplateTask(
                        template.getId(), item.title(), item.description(),
                        item.assigneeRole(), item.sortOrder()
                )))
                .toList();

        return toTemplateResponse(template, tasks);
    }

    public List<OnboardingTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(template -> toTemplateResponse(
                        template,
                        templateTaskRepository.findByTemplateIdOrderBySortOrderAsc(template.getId())
                ))
                .toList();
    }

    @Transactional
    public OnboardingWorkflowResponse startWorkflow(UUID employeeId, UUID templateId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        OnboardingTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() ->
                        new OnboardingNotFoundException("Onboarding template not found: " + templateId)
                );

        if (workflowRepository.existsByEmployeeIdAndStatus(
                employeeId, OnboardingWorkflow.STATUS_IN_PROGRESS)) {
            throw new BusinessRuleException(
                    "Employee already has an onboarding workflow in progress"
            );
        }

        OnboardingWorkflow workflow = workflowRepository.save(
                new OnboardingWorkflow(employeeId, templateId)
        );

        List<OnboardingTemplateTask> templateTasks =
                templateTaskRepository.findByTemplateIdOrderBySortOrderAsc(templateId);

        templateTasks.forEach(templateTask -> taskRepository.save(new OnboardingTask(
                workflow.getId(), templateTask.getTitle(), templateTask.getDescription(),
                templateTask.getAssigneeRole(), templateTask.getSortOrder()
        )));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.ONBOARDING_STARTED,
                "Onboarding started",
                "Your onboarding checklist \"" + template.getName() + "\" has been started."
        );

        return OnboardingWorkflowResponse.from(workflow);
    }

    public List<OnboardingWorkflowResponse> getWorkflowsForEmployee(UUID employeeId) {
        return workflowRepository.findByEmployeeId(employeeId).stream()
                .map(OnboardingWorkflowResponse::from)
                .toList();
    }

    public List<OnboardingTaskResponse> getWorkflowTasks(UUID workflowId) {
        return taskRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId).stream()
                .map(OnboardingTaskResponse::from)
                .toList();
    }

    public List<OnboardingTaskResponse> getOwnTasks(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return taskRepository.findByEmployeeId(employee.getId()).stream()
                .map(OnboardingTaskResponse::from)
                .toList();
    }

    @Transactional
    public OnboardingTaskResponse completeTask(
            UUID taskId,
            UUID requesterUserId,
            boolean requesterCanManageOnboarding
    ) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new OnboardingNotFoundException("Onboarding task not found: " + taskId));

        OnboardingWorkflow workflow = workflowRepository.findById(task.getWorkflowId())
                .orElseThrow(() ->
                        new OnboardingNotFoundException("Onboarding workflow not found: " + task.getWorkflowId())
                );

        if (!requesterCanManageOnboarding) {

            Employee employee = employeeRepository.findById(workflow.getEmployeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));

            if (!employee.getUserId().equals(requesterUserId)) {
                throw new AccessDeniedException(
                        "Only the employee or HR can complete this onboarding task"
                );
            }
        }

        if (task.isCompleted()) {
            return OnboardingTaskResponse.from(task);
        }

        task.complete(requesterUserId);

        boolean allTasksCompleted = !taskRepository.existsByWorkflowIdAndStatusNot(
                workflow.getId(), OnboardingTask.STATUS_COMPLETED
        );

        if (allTasksCompleted && OnboardingWorkflow.STATUS_IN_PROGRESS.equals(workflow.getStatus())) {

            workflow.complete();

            employeeRepository.findById(workflow.getEmployeeId()).ifPresent(employee ->
                    notificationPublisher.publish(
                            employee.getUserId(),
                            NotificationType.ONBOARDING_COMPLETED,
                            "Onboarding complete",
                            "You have completed all of your onboarding tasks. Welcome aboard!"
                    )
            );
        }

        return OnboardingTaskResponse.from(task);
    }

    private OnboardingTemplateResponse toTemplateResponse(
            OnboardingTemplate template,
            List<OnboardingTemplateTask> tasks
    ) {
        List<OnboardingTemplateTaskResponse> taskResponses = tasks.stream()
                .map(OnboardingTemplateTaskResponse::from)
                .toList();

        return OnboardingTemplateResponse.from(template, taskResponses);
    }
}
