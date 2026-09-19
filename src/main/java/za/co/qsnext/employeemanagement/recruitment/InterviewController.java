package za.co.qsnext.employeemanagement.recruitment;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.recruitment.dto.InterviewFeedbackResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.InterviewResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.ScheduleInterviewRequest;
import za.co.qsnext.employeemanagement.recruitment.dto.SubmitInterviewFeedbackRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Recruitment - Interviews", description = "Interview scheduling and feedback.")
@RestController
@RequestMapping("/api/v1/recruitment/interviews")
public class InterviewController {

    private static final String MANAGE_AUTHORITY = "RECRUITMENT_MANAGE";

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PreAuthorize("hasAuthority('RECRUITMENT_MANAGE')")
    @Operation(summary = "Schedule interview")
    @PostMapping
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Valid @RequestBody ScheduleInterviewRequest request
    ) {
        InterviewResponse response = interviewService.scheduleInterview(
                request.applicationId(), request.interviewerUserId(), request.scheduledAt(),
                request.durationMinutes(), request.location()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('RECRUITMENT_MANAGE')")
    @Operation(summary = "Get interviews for application")
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<List<InterviewResponse>> getInterviewsForApplication(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(interviewService.getInterviewsForApplication(applicationId));
    }

    @PreAuthorize("hasAuthority('RECRUITMENT_INTERVIEWER')")
    @Operation(summary = "Get my interviews")
    @GetMapping("/my-interviews")
    public ResponseEntity<List<InterviewResponse>> getMyInterviews(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(interviewService.getMyInterviews(userDetails.getUserId()));
    }

    @PreAuthorize("hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_INTERVIEWER')")
    @Operation(summary = "Complete interview")
    @PatchMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewResponse> completeInterview(
            Authentication authentication,
            @PathVariable UUID interviewId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(interviewService.completeInterview(
                interviewId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_INTERVIEWER')")
    @Operation(summary = "Cancel interview")
    @PatchMapping("/{interviewId}/cancel")
    public ResponseEntity<InterviewResponse> cancelInterview(
            Authentication authentication,
            @PathVariable UUID interviewId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(interviewService.cancelInterview(
                interviewId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('RECRUITMENT_MANAGE', 'RECRUITMENT_INTERVIEWER')")
    @Operation(summary = "Submit feedback")
    @PostMapping("/{interviewId}/feedback")
    public ResponseEntity<InterviewFeedbackResponse> submitFeedback(
            Authentication authentication,
            @PathVariable UUID interviewId,
            @Valid @RequestBody SubmitInterviewFeedbackRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        InterviewFeedbackResponse response = interviewService.submitFeedback(
                interviewId, userDetails.getUserId(), canManage(authentication),
                request.rating(), request.comments(), request.recommendation()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('RECRUITMENT_MANAGE')")
    @Operation(summary = "Get feedback for application")
    @GetMapping("/applications/{applicationId}/feedback")
    public ResponseEntity<List<InterviewFeedbackResponse>> getFeedbackForApplication(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(interviewService.getFeedbackForApplication(applicationId));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
