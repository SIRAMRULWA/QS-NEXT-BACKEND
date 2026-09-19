package za.co.qsnext.employeemanagement.performance;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.performance.dto.*;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Performance", description = "Performance cycles, goals, reviews and development plans.")
@RestController
@RequestMapping("/api/v1/performance")
public class PerformanceController {

    private static final String MANAGE_AUTHORITY = "PERFORMANCE_MANAGE";

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @PreAuthorize("hasAuthority('PERFORMANCE_MANAGE')")
    @Operation(summary = "Create cycle")
    @PostMapping("/cycles")
    public ResponseEntity<PerformanceCycleResponse> createCycle(
            @Valid @RequestBody CreatePerformanceCycleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                performanceService.createCycle(request.name(), request.startDate(), request.endDate())
        );
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Get all cycles")
    @GetMapping("/cycles")
    public ResponseEntity<List<PerformanceCycleResponse>> getAllCycles() {
        return ResponseEntity.ok(performanceService.getAllCycles());
    }

    @PreAuthorize("hasAuthority('PERFORMANCE_MANAGE')")
    @Operation(summary = "Create goal")
    @PostMapping("/employees/{employeeId}/goals")
    public ResponseEntity<PerformanceGoalResponse> createGoal(
            @PathVariable UUID employeeId,
            @RequestParam UUID cycleId,
            @Valid @RequestBody CreatePerformanceGoalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createGoal(
                cycleId, employeeId, request.title(), request.description(), request.targetDate()
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Get goals for employee")
    @GetMapping("/employees/{employeeId}/goals")
    public ResponseEntity<List<PerformanceGoalResponse>> getGoalsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.getGoalsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Update goal status")
    @PatchMapping("/goals/{goalId}/status")
    public ResponseEntity<PerformanceGoalResponse> updateGoalStatus(
            Authentication authentication,
            @PathVariable UUID goalId,
            @RequestParam String status
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.updateGoalStatus(
                goalId, status, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('PERFORMANCE_MANAGE')")
    @Operation(summary = "Create review")
    @PostMapping("/reviews")
    public ResponseEntity<PerformanceReviewResponse> createReview(
            @Valid @RequestBody CreatePerformanceReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createReview(
                request.cycleId(), request.employeeId(), request.reviewerUserId()
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Get review")
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<PerformanceReviewResponse> getReview(
            Authentication authentication,
            @PathVariable UUID reviewId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.getReview(
                reviewId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Get reviews for employee")
    @GetMapping("/employees/{employeeId}/reviews")
    public ResponseEntity<List<PerformanceReviewResponse>> getReviewsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.getReviewsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('PERFORMANCE_READ')")
    @Operation(summary = "Submit self assessment")
    @PatchMapping("/reviews/{reviewId}/self-assessment")
    public ResponseEntity<PerformanceReviewResponse> submitSelfAssessment(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody SubmitAssessmentRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.submitSelfAssessment(
                reviewId, userDetails.getUserId(), request.rating(), request.comments()
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Submit manager assessment")
    @PatchMapping("/reviews/{reviewId}/manager-assessment")
    public ResponseEntity<PerformanceReviewResponse> submitManagerAssessment(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody SubmitAssessmentRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.submitManagerAssessment(
                reviewId, userDetails.getUserId(), canManage(authentication), request.rating(), request.comments()
        ));
    }

    @PreAuthorize("hasAuthority('PERFORMANCE_MANAGE')")
    @Operation(summary = "Create development plan")
    @PostMapping("/employees/{employeeId}/development-plans")
    public ResponseEntity<DevelopmentPlanResponse> createDevelopmentPlan(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateDevelopmentPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createDevelopmentPlan(
                employeeId, request.reviewId(), request.description(),
                request.recommendedCourseId(), request.targetDate()
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Get development plans for employee")
    @GetMapping("/employees/{employeeId}/development-plans")
    public ResponseEntity<List<DevelopmentPlanResponse>> getDevelopmentPlansForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.getDevelopmentPlansForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('PERFORMANCE_MANAGE', 'PERFORMANCE_READ')")
    @Operation(summary = "Complete development plan")
    @PatchMapping("/development-plans/{planId}/complete")
    public ResponseEntity<DevelopmentPlanResponse> completeDevelopmentPlan(
            Authentication authentication,
            @PathVariable UUID planId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(performanceService.completeDevelopmentPlan(
                planId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
