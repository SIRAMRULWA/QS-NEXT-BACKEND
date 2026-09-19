package za.co.qsnext.employeemanagement.recruitment;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.recruitment.dto.*;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recruitment")
@PreAuthorize("hasAuthority('RECRUITMENT_MANAGE')")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @PostMapping("/requisitions")
    public ResponseEntity<JobRequisitionResponse> createRequisition(
            Authentication authentication,
            @Valid @RequestBody CreateJobRequisitionRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        JobRequisitionResponse response = recruitmentService.createRequisition(
                request.title(), request.departmentId(), request.description(),
                request.numberOfOpenings(), userDetails.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requisitions")
    public ResponseEntity<List<JobRequisitionResponse>> getAllRequisitions() {
        return ResponseEntity.ok(recruitmentService.getAllRequisitions());
    }

    @PatchMapping("/requisitions/{requisitionId}/close")
    public ResponseEntity<JobRequisitionResponse> closeRequisition(@PathVariable UUID requisitionId) {
        return ResponseEntity.ok(recruitmentService.closeRequisition(requisitionId));
    }

    @PostMapping("/requisitions/{requisitionId}/postings")
    public ResponseEntity<JobPostingResponse> createPosting(
            @PathVariable UUID requisitionId,
            @Valid @RequestBody CreateJobPostingRequest request
    ) {
        JobPostingResponse response = recruitmentService.createPosting(
                requisitionId, request.title(), request.description(), request.location(), request.employmentType()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/postings")
    public ResponseEntity<List<JobPostingResponse>> getOpenPostings() {
        return ResponseEntity.ok(recruitmentService.getOpenPostings());
    }

    @PatchMapping("/postings/{postingId}/close")
    public ResponseEntity<JobPostingResponse> closePosting(@PathVariable UUID postingId) {
        return ResponseEntity.ok(recruitmentService.closePosting(postingId));
    }

    @PostMapping("/candidates")
    public ResponseEntity<CandidateResponse> createCandidate(@Valid @RequestBody CreateCandidateRequest request) {
        CandidateResponse response = recruitmentService.createCandidate(
                request.firstName(), request.lastName(), request.email(),
                request.phone(), request.resumeDocumentId(), request.source()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<CandidateResponse> getCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(recruitmentService.getCandidate(candidateId));
    }

    @GetMapping("/candidates/{candidateId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(recruitmentService.getApplicationsForCandidate(candidateId));
    }

    @PostMapping("/applications")
    public ResponseEntity<ApplicationResponse> createApplication(
            @RequestParam UUID candidateId,
            @RequestParam UUID jobPostingId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                recruitmentService.createApplication(candidateId, jobPostingId)
        );
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(recruitmentService.getApplication(applicationId));
    }

    @GetMapping("/postings/{postingId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForPosting(@PathVariable UUID postingId) {
        return ResponseEntity.ok(recruitmentService.getApplicationsForPosting(postingId));
    }

    @PatchMapping("/applications/{applicationId}/advance")
    public ResponseEntity<ApplicationResponse> advanceApplication(
            @PathVariable UUID applicationId,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(recruitmentService.advanceApplication(applicationId, status));
    }

    @PatchMapping("/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable UUID applicationId,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        return ResponseEntity.ok(recruitmentService.rejectApplication(applicationId, request.reason()));
    }

    @PatchMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<ApplicationResponse> withdrawApplication(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(recruitmentService.withdrawApplication(applicationId));
    }
}
