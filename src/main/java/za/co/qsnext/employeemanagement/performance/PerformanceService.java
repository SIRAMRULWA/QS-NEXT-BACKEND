package za.co.qsnext.employeemanagement.performance;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.PerformanceNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.performance.dto.DevelopmentPlanResponse;
import za.co.qsnext.employeemanagement.performance.dto.PerformanceCycleResponse;
import za.co.qsnext.employeemanagement.performance.dto.PerformanceGoalResponse;
import za.co.qsnext.employeemanagement.performance.dto.PerformanceReviewResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceCycleRepository cycleRepository;
    private final PerformanceGoalRepository goalRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final DevelopmentPlanRepository developmentPlanRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditService auditService;

    public PerformanceService(
            PerformanceCycleRepository cycleRepository,
            PerformanceGoalRepository goalRepository,
            PerformanceReviewRepository reviewRepository,
            DevelopmentPlanRepository developmentPlanRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher,
            AuditService auditService
    ) {
        this.cycleRepository = cycleRepository;
        this.goalRepository = goalRepository;
        this.reviewRepository = reviewRepository;
        this.developmentPlanRepository = developmentPlanRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public PerformanceCycleResponse createCycle(String name, LocalDate startDate, LocalDate endDate) {

        if (cycleRepository.existsByName(name)) {
            throw new DuplicateResourceException("Performance cycle already exists: " + name);
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        return PerformanceCycleResponse.from(
                cycleRepository.save(new PerformanceCycle(name, startDate, endDate))
        );
    }

    public List<PerformanceCycleResponse> getAllCycles() {
        return cycleRepository.findAll().stream().map(PerformanceCycleResponse::from).toList();
    }

    @Transactional
    public PerformanceGoalResponse createGoal(
            UUID cycleId,
            UUID employeeId,
            String title,
            String description,
            LocalDate targetDate
    ) {
        PerformanceCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new PerformanceNotFoundException("Performance cycle not found: " + cycleId));

        if (!cycle.isOpen()) {
            throw new BusinessRuleException("Cannot add goals to a closed performance cycle");
        }

        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        }

        return PerformanceGoalResponse.from(
                goalRepository.save(new PerformanceGoal(cycleId, employeeId, title, description, targetDate))
        );
    }

    public List<PerformanceGoalResponse> getGoalsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeePerformance(employeeId, requesterUserId, requesterCanManage);

        return goalRepository.findByEmployeeId(employeeId).stream().map(PerformanceGoalResponse::from).toList();
    }

    @Transactional
    public PerformanceGoalResponse updateGoalStatus(
            UUID goalId,
            String status,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        PerformanceGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new PerformanceNotFoundException("Performance goal not found: " + goalId));

        assertCanAccessEmployeePerformance(goal.getEmployeeId(), requesterUserId, requesterCanManage);

        goal.updateStatus(status);

        return PerformanceGoalResponse.from(goal);
    }

    @Transactional
    public PerformanceReviewResponse createReview(UUID cycleId, UUID employeeId, UUID reviewerUserId) {

        if (!cycleRepository.existsById(cycleId)) {
            throw new PerformanceNotFoundException("Performance cycle not found: " + cycleId);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (reviewRepository.existsByCycleIdAndEmployeeId(cycleId, employeeId)) {
            throw new BusinessRuleException("Employee already has a review for this cycle");
        }

        UUID resolvedReviewerUserId = reviewerUserId != null
                ? reviewerUserId
                : resolveManagerUserId(employee);

        PerformanceReview review = reviewRepository.save(
                new PerformanceReview(cycleId, employeeId, resolvedReviewerUserId)
        );

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.PERFORMANCE_REVIEW_ASSIGNED,
                "Performance review started",
                "A new performance review has been started for you. Please submit your self-assessment."
        );

        auditService.log("PERFORMANCE_REVIEW_CREATED", "PerformanceReview", review.getId(), AuditService.RESULT_SUCCESS);

        return PerformanceReviewResponse.from(review);
    }

    public PerformanceReviewResponse getReview(UUID reviewId, UUID requesterUserId, boolean requesterCanManage) {

        PerformanceReview review = findReviewOrThrow(reviewId);
        assertCanAccessReview(review, requesterUserId, requesterCanManage);

        return PerformanceReviewResponse.from(review);
    }

    public List<PerformanceReviewResponse> getReviewsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeePerformance(employeeId, requesterUserId, requesterCanManage);

        return reviewRepository.findByEmployeeId(employeeId).stream()
                .map(PerformanceReviewResponse::from)
                .toList();
    }

    @Transactional
    public PerformanceReviewResponse submitSelfAssessment(
            UUID reviewId,
            UUID requesterUserId,
            Integer rating,
            String comments
    ) {
        PerformanceReview review = findReviewOrThrow(reviewId);
        Employee employee = employeeRepository.findById(review.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + review.getEmployeeId()));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Only the reviewed employee can submit this self-assessment");
        }

        review.submitSelfAssessment(rating, comments);
        notifyIfCompleted(review, employee);

        return PerformanceReviewResponse.from(review);
    }

    @Transactional
    public PerformanceReviewResponse submitManagerAssessment(
            UUID reviewId,
            UUID requesterUserId,
            boolean requesterCanManage,
            Integer rating,
            String comments
    ) {
        PerformanceReview review = findReviewOrThrow(reviewId);

        if (!requesterCanManage && !review.getReviewerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Only the assigned reviewer can submit this manager assessment");
        }

        review.submitManagerAssessment(rating, comments);

        Employee employee = employeeRepository.findById(review.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + review.getEmployeeId()));

        notifyIfCompleted(review, employee);

        return PerformanceReviewResponse.from(review);
    }

    @Transactional
    public DevelopmentPlanResponse createDevelopmentPlan(
            UUID employeeId,
            UUID reviewId,
            String description,
            UUID recommendedCourseId,
            LocalDate targetDate
    ) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        }

        if (reviewId != null && !reviewRepository.existsById(reviewId)) {
            throw new PerformanceNotFoundException("Performance review not found: " + reviewId);
        }

        DevelopmentPlan plan = developmentPlanRepository.save(
                new DevelopmentPlan(employeeId, reviewId, description, recommendedCourseId, targetDate)
        );

        auditService.log("DEVELOPMENT_PLAN_CREATED", "DevelopmentPlan", plan.getId(), AuditService.RESULT_SUCCESS);

        return DevelopmentPlanResponse.from(plan);
    }

    public List<DevelopmentPlanResponse> getDevelopmentPlansForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeePerformance(employeeId, requesterUserId, requesterCanManage);

        return developmentPlanRepository.findByEmployeeId(employeeId).stream()
                .map(DevelopmentPlanResponse::from)
                .toList();
    }

    @Transactional
    public DevelopmentPlanResponse completeDevelopmentPlan(
            UUID planId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        DevelopmentPlan plan = developmentPlanRepository.findById(planId)
                .orElseThrow(() -> new PerformanceNotFoundException("Development plan not found: " + planId));

        assertCanAccessEmployeePerformance(plan.getEmployeeId(), requesterUserId, requesterCanManage);

        plan.complete();

        return DevelopmentPlanResponse.from(plan);
    }

    private void notifyIfCompleted(PerformanceReview review, Employee employee) {
        if (review.isFullyCompleted()) {
            notificationPublisher.publish(
                    employee.getUserId(),
                    NotificationType.PERFORMANCE_REVIEW_COMPLETED,
                    "Performance review completed",
                    "Your performance review is complete. Both assessments have been submitted."
            );
            auditService.log(
                    "PERFORMANCE_REVIEW_COMPLETED", "PerformanceReview", review.getId(), AuditService.RESULT_SUCCESS
            );
        }
    }

    private UUID resolveManagerUserId(Employee employee) {

        UUID managerId = employee.getManagerId();

        if (managerId == null) {
            throw new BusinessRuleException(
                    "Employee has no manager assigned; a reviewer must be specified explicitly"
            );
        }

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new EmployeeNotFoundException("Manager not found: " + managerId));

        return manager.getUserId();
    }

    private PerformanceReview findReviewOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new PerformanceNotFoundException("Performance review not found: " + reviewId));
    }

    private void assertCanAccessReview(PerformanceReview review, UUID requesterUserId, boolean requesterCanManage) {

        if (requesterCanManage || review.getReviewerUserId().equals(requesterUserId)) {
            return;
        }

        Employee employee = employeeRepository.findById(review.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + review.getEmployeeId()));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to view this performance review");
        }
    }

    private void assertCanAccessEmployeePerformance(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to access this employee's performance records");
        }
    }
}
