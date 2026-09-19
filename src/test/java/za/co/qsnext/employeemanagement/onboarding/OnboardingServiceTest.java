package za.co.qsnext.employeemanagement.onboarding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.OnboardingNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateTaskItem;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTemplateResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingWorkflowResponse;
import za.co.qsnext.employeemanagement.onboarding.dto.OnboardingTaskResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private OnboardingTemplateRepository templateRepository;
    @Mock
    private OnboardingTemplateTaskRepository templateTaskRepository;
    @Mock
    private OnboardingWorkflowRepository workflowRepository;
    @Mock
    private OnboardingTaskRepository taskRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;

    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(
                templateRepository, templateTaskRepository, workflowRepository,
                taskRepository, employeeRepository, notificationPublisher);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    @Test
    void createTemplate_savesTemplateAndTasks() {
        when(templateRepository.existsByName("Standard Onboarding")).thenReturn(false);
        when(templateRepository.save(any())).thenAnswer(invocation -> {
            OnboardingTemplate template = invocation.getArgument(0);
            setId(template, UUID.randomUUID());
            return template;
        });
        when(templateTaskRepository.save(any())).thenAnswer(invocation -> {
            OnboardingTemplateTask task = invocation.getArgument(0);
            setId(task, UUID.randomUUID());
            return task;
        });

        List<OnboardingTemplateTaskItem> items = List.of(
                new OnboardingTemplateTaskItem("Sign contract", null, "HR", 1),
                new OnboardingTemplateTaskItem("Setup laptop", null, "MANAGER", 2)
        );

        OnboardingTemplateResponse response =
                onboardingService.createTemplate("Standard Onboarding", "Desc", items);

        assertThat(response.name()).isEqualTo("Standard Onboarding");
        assertThat(response.tasks()).hasSize(2);
    }

    @Test
    void createTemplate_rejectsADuplicateName() {
        when(templateRepository.existsByName("Standard Onboarding")).thenReturn(true);

        assertThatThrownBy(() -> onboardingService.createTemplate(
                "Standard Onboarding", "Desc", List.of()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(templateRepository, never()).save(any());
    }

    @Test
    void startWorkflow_copiesTemplateTasksAndNotifiesEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        Employee employee = employeeWithId(employeeId, userId);
        OnboardingTemplate template = new OnboardingTemplate("Standard Onboarding", "Desc");
        setId(template, templateId);

        OnboardingTemplateTask templateTask = new OnboardingTemplateTask(
                templateId, "Sign contract", null, OnboardingTemplateTask.ROLE_HR, 1);
        setId(templateTask, UUID.randomUUID());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(workflowRepository.existsByEmployeeIdAndStatus(
                employeeId, OnboardingWorkflow.STATUS_IN_PROGRESS)).thenReturn(false);
        when(workflowRepository.save(any())).thenAnswer(invocation -> {
            OnboardingWorkflow workflow = invocation.getArgument(0);
            setId(workflow, UUID.randomUUID());
            return workflow;
        });
        when(templateTaskRepository.findByTemplateIdOrderBySortOrderAsc(templateId))
                .thenReturn(List.of(templateTask));
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingWorkflowResponse response = onboardingService.startWorkflow(employeeId, templateId);

        assertThat(response.status()).isEqualTo(OnboardingWorkflow.STATUS_IN_PROGRESS);
        verify(taskRepository).save(any());
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.ONBOARDING_STARTED), any(), any());
    }

    @Test
    void startWorkflow_rejectsWhenAWorkflowIsAlreadyInProgress() {
        UUID employeeId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        OnboardingTemplate template = new OnboardingTemplate("Standard Onboarding", "Desc");
        setId(template, templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(workflowRepository.existsByEmployeeIdAndStatus(
                employeeId, OnboardingWorkflow.STATUS_IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> onboardingService.startWorkflow(employeeId, templateId))
                .isInstanceOf(BusinessRuleException.class);

        verify(workflowRepository, never()).save(any());
        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void startWorkflow_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startWorkflow(employeeId, UUID.randomUUID()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void startWorkflow_throws_whenTemplateDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startWorkflow(employeeId, templateId))
                .isInstanceOf(OnboardingNotFoundException.class);
    }

    @Test
    void completeTask_isAllowed_fortheOwningEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        OnboardingTask task = new OnboardingTask(
                workflowId, "Sign contract", null, OnboardingTemplateTask.ROLE_EMPLOYEE, 1);
        setId(task, taskId);

        OnboardingWorkflow workflow = new OnboardingWorkflow(employeeId, UUID.randomUUID());
        setId(workflow, workflowId);

        Employee employee = employeeWithId(employeeId, userId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(taskRepository.existsByWorkflowIdAndStatusNot(workflowId, OnboardingTask.STATUS_COMPLETED))
                .thenReturn(false);

        OnboardingTaskResponse response = onboardingService.completeTask(taskId, userId, false);

        assertThat(response.status()).isEqualTo(OnboardingTask.STATUS_COMPLETED);
        assertThat(workflow.getStatus()).isEqualTo(OnboardingWorkflow.STATUS_COMPLETED);
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.ONBOARDING_COMPLETED), any(), any());
    }

    @Test
    void completeTask_isDenied_forANonOwningNonManagingRequester() {
        UUID employeeId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        OnboardingTask task = new OnboardingTask(
                workflowId, "Sign contract", null, OnboardingTemplateTask.ROLE_EMPLOYEE, 1);
        setId(task, taskId);

        OnboardingWorkflow workflow = new OnboardingWorkflow(employeeId, UUID.randomUUID());
        setId(workflow, workflowId);

        Employee employee = employeeWithId(employeeId, UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() ->
                onboardingService.completeTask(taskId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void completeTask_isIdempotent_whenAlreadyCompleted() {
        UUID employeeId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        OnboardingTask task = new OnboardingTask(
                workflowId, "Sign contract", null, OnboardingTemplateTask.ROLE_HR, 1);
        setId(task, taskId);
        task.complete(UUID.randomUUID());

        OnboardingWorkflow workflow = new OnboardingWorkflow(employeeId, UUID.randomUUID());
        setId(workflow, workflowId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        OnboardingTaskResponse response = onboardingService.completeTask(taskId, UUID.randomUUID(), true);

        assertThat(response.status()).isEqualTo(OnboardingTask.STATUS_COMPLETED);
        verify(taskRepository, never()).existsByWorkflowIdAndStatusNot(any(), any());
    }

    @Test
    void completeTask_leavesWorkflowInProgress_whenOtherTasksRemain() {
        UUID employeeId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        OnboardingTask task = new OnboardingTask(
                workflowId, "Sign contract", null, OnboardingTemplateTask.ROLE_HR, 1);
        setId(task, taskId);

        OnboardingWorkflow workflow = new OnboardingWorkflow(employeeId, UUID.randomUUID());
        setId(workflow, workflowId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(taskRepository.existsByWorkflowIdAndStatusNot(workflowId, OnboardingTask.STATUS_COMPLETED))
                .thenReturn(true);

        OnboardingTaskResponse response = onboardingService.completeTask(taskId, UUID.randomUUID(), true);

        assertThat(response.status()).isEqualTo(OnboardingTask.STATUS_COMPLETED);
        assertThat(workflow.getStatus()).isEqualTo(OnboardingWorkflow.STATUS_IN_PROGRESS);
        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void completeTask_throws_whenTaskDoesNotExist() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.completeTask(taskId, UUID.randomUUID(), true))
                .isInstanceOf(OnboardingNotFoundException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
