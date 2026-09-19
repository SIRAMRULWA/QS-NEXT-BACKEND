package za.co.qsnext.employeemanagement.learning;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.learning.dto.CourseEnrollmentResponse;
import za.co.qsnext.employeemanagement.learning.dto.CourseResponse;
import za.co.qsnext.employeemanagement.learning.dto.CreateCourseRequest;
import za.co.qsnext.employeemanagement.learning.dto.CreateLearningPathRequest;
import za.co.qsnext.employeemanagement.learning.dto.LearningPathResponse;
import za.co.qsnext.employeemanagement.learning.dto.UpdateProgressRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Learning", description = "Courses, enrollments, learning paths and employee skills.")
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private static final String MANAGE_AUTHORITY = "LEARNING_MANAGE";

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PreAuthorize("hasAuthority('LEARNING_MANAGE')")
    @Operation(summary = "Create course")
    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learningService.createCourse(
                request.title(), request.description(), request.category(),
                request.durationMinutes(), request.mandatory(), request.linkedComplianceRequirementId()
        ));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Get active courses")
    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getActiveCourses() {
        return ResponseEntity.ok(learningService.getActiveCourses());
    }

    @PreAuthorize("hasAuthority('LEARNING_MANAGE')")
    @Operation(summary = "Create learning path")
    @PostMapping("/paths")
    public ResponseEntity<LearningPathResponse> createLearningPath(
            @Valid @RequestBody CreateLearningPathRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                learningService.createLearningPath(request.name(), request.description(), request.courseIds())
        );
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Get all learning paths")
    @GetMapping("/paths")
    public ResponseEntity<List<LearningPathResponse>> getAllLearningPaths() {
        return ResponseEntity.ok(learningService.getAllLearningPaths());
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Enroll")
    @PostMapping("/employees/{employeeId}/enrollments")
    public ResponseEntity<CourseEnrollmentResponse> enroll(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @RequestParam UUID courseId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED).body(learningService.enroll(
                employeeId, courseId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Get enrollments for employee")
    @GetMapping("/employees/{employeeId}/enrollments")
    public ResponseEntity<List<CourseEnrollmentResponse>> getEnrollmentsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(learningService.getEnrollmentsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('LEARNING_READ')")
    @Operation(summary = "Get my enrollments")
    @GetMapping("/my-enrollments")
    public ResponseEntity<List<CourseEnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(learningService.getMyEnrollments(userDetails.getUserId()));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Update progress")
    @PatchMapping("/enrollments/{enrollmentId}/progress")
    public ResponseEntity<CourseEnrollmentResponse> updateProgress(
            Authentication authentication,
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(learningService.updateProgress(
                enrollmentId, request.progressPercent(), userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Complete course")
    @PatchMapping("/enrollments/{enrollmentId}/complete")
    public ResponseEntity<CourseEnrollmentResponse> completeCourse(
            Authentication authentication,
            @PathVariable UUID enrollmentId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(learningService.completeCourse(
                enrollmentId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
