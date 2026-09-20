package za.co.qsnext.employeemanagement.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.performance.dto.PerformanceReviewResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceCycleRepository cycleRepository;
    @Mock
    private PerformanceGoalRepository goalRepository;
    @Mock
    private PerformanceReviewRepository reviewRepository;
    @Mock
    private DevelopmentPlanRepository developmentPlanRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private AuditService auditService;

    private PerformanceService performanceService;

    @BeforeEach
    void setUp() {
        performanceService = new PerformanceService(
                cycleRepository, goalRepository, reviewRepository, developmentPlanRepository,
                employeeRepository, notificationPublisher, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId, UUID managerId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        if (managerId != null) {
            employee.assignManager(managerId);
        }
        return employee;
    }

    @Test
    void createCycle_rejectsAnEndDateBeforeStartDate() {
        when(cycleRepository.existsByName("2026 H1")).thenReturn(false);

        LocalDate start = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> performanceService.createCycle("2026 H1", start, start.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createReview_resolvesReviewerFromEmployeesManager_whenNotSpecified() {
        UUID cycleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();

        Employee employee = employeeWithId(employeeId, UUID.randomUUID(), managerId);
        Employee manager = employeeWithId(managerId, managerUserId, null);

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(reviewRepository.existsByCycleIdAndEmployeeId(cycleId, employeeId)).thenReturn(false);
        when(reviewRepository.save(any())).thenAnswer(invocation -> {
            PerformanceReview review = invocation.getArgument(0);
            setId(review, UUID.randomUUID());
            return review;
        });

        PerformanceReviewResponse response = performanceService.createReview(cycleId, employeeId, null);

        assertThat(response.reviewerUserId()).isEqualTo(managerUserId);
        verify(notificationPublisher).publish(
                eq(employee.getUserId()), eq(NotificationType.PERFORMANCE_REVIEW_ASSIGNED), any(), any());
    }

    @Test
    void createReview_throws_whenEmployeeHasNoManagerAndNoneSpecified() {
        UUID cycleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), null)));
        when(reviewRepository.existsByCycleIdAndEmployeeId(cycleId, employeeId)).thenReturn(false);

        assertThatThrownBy(() -> performanceService.createReview(cycleId, employeeId, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createReview_rejectsADuplicateReviewForTheSameCycle() {
        UUID cycleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(cycleRepository.existsById(cycleId)).thenReturn(true);
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), null)));
        when(reviewRepository.existsByCycleIdAndEmployeeId(cycleId, employeeId)).thenReturn(true);

        assertThatThrownBy(() -> performanceService.createReview(cycleId, employeeId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void submitSelfAssessment_isDenied_forSomeoneOtherThanTheReviewedEmployee() {
        UUID reviewId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        PerformanceReview review = new PerformanceReview(UUID.randomUUID(), employeeId, UUID.randomUUID());
        setId(review, reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), null)));

        assertThatThrownBy(() -> performanceService.submitSelfAssessment(
                reviewId, UUID.randomUUID(), 4, "Good progress"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void submittingBothAssessments_completesTheReviewAndNotifies() {
        UUID reviewId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID employeeUserId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();

        PerformanceReview review = new PerformanceReview(UUID.randomUUID(), employeeId, reviewerUserId);
        setId(review, reviewId);
        Employee employee = employeeWithId(employeeId, employeeUserId, null);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        performanceService.submitSelfAssessment(reviewId, employeeUserId, 4, "Solid quarter");
        assertThat(review.getStatus()).isEqualTo(PerformanceReview.STATUS_IN_PROGRESS);

        PerformanceReviewResponse response =
                performanceService.submitManagerAssessment(reviewId, reviewerUserId, false, 5, "Great work");

        assertThat(response.status()).isEqualTo(PerformanceReview.STATUS_COMPLETED);
        verify(notificationPublisher).publish(
                eq(employeeUserId), eq(NotificationType.PERFORMANCE_REVIEW_COMPLETED), any(), any());
    }

    @Test
    void submitManagerAssessment_isDenied_forSomeoneOtherThanTheAssignedReviewer() {
        UUID reviewId = UUID.randomUUID();
        PerformanceReview review = new PerformanceReview(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        setId(review, reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> performanceService.submitManagerAssessment(
                reviewId, UUID.randomUUID(), false, 3, "Comments"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void completeDevelopmentPlan_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID planId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        DevelopmentPlan plan = new DevelopmentPlan(employeeId, null, "Take a course", null, null);
        setId(plan, planId);

        when(developmentPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), null)));

        assertThatThrownBy(() -> performanceService.completeDevelopmentPlan(planId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void completeDevelopmentPlan_isAllowed_forAManager() {
        UUID planId = UUID.randomUUID();
        DevelopmentPlan plan = new DevelopmentPlan(UUID.randomUUID(), null, "Take a course", null, null);
        setId(plan, planId);

        when(developmentPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        var response = performanceService.completeDevelopmentPlan(planId, UUID.randomUUID(), true);

        assertThat(response.status()).isEqualTo(DevelopmentPlan.STATUS_COMPLETED);
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
