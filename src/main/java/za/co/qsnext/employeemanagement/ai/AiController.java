package za.co.qsnext.employeemanagement.ai;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.ai.dto.AiSuggestionResponse;
import za.co.qsnext.employeemanagement.ai.dto.AskHrAssistantRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.UUID;

@Tag(name = "AI", description = "AI-powered HR capabilities: assistant, document summarization, recruitment drafting, recommendations.")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final String HR_TOOLS_AUTHORITY = "AI_HR_TOOLS_USE";

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @Operation(summary = "Ask hr assistant")
    @PostMapping("/assistant")
    public ResponseEntity<AiSuggestionResponse> askHrAssistant(
            Authentication authentication,
            @Valid @RequestBody AskHrAssistantRequest request
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.askHrAssistant(request.question(), userId));
    }

    @PreAuthorize("hasAuthority('AI_HR_TOOLS_USE')")
    @Operation(summary = "Summarize document")
    @PostMapping("/documents/{documentId}/summarize")
    public ResponseEntity<AiSuggestionResponse> summarizeDocument(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.summarizeDocument(documentId, userId));
    }

    @PreAuthorize("hasAuthority('AI_HR_TOOLS_USE')")
    @Operation(summary = "Generate job description")
    @PostMapping("/recruitment/job-requisitions/{jobRequisitionId}/generate-description")
    public ResponseEntity<AiSuggestionResponse> generateJobDescription(
            Authentication authentication,
            @PathVariable UUID jobRequisitionId
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.generateJobDescription(jobRequisitionId, userId));
    }

    @PreAuthorize("hasAuthority('AI_HR_TOOLS_USE')")
    @Operation(summary = "Match candidates")
    @PostMapping("/recruitment/job-postings/{jobPostingId}/match-candidates")
    public ResponseEntity<AiSuggestionResponse> matchCandidates(
            Authentication authentication,
            @PathVariable UUID jobPostingId
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.matchCandidates(jobPostingId, userId));
    }

    @PreAuthorize("hasAnyAuthority('AI_RECOMMENDATIONS_READ', 'AI_HR_TOOLS_USE')")
    @Operation(summary = "Recommend skills")
    @PostMapping("/employees/{employeeId}/recommend-skills")
    public ResponseEntity<AiSuggestionResponse> recommendSkills(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(aiService.recommendSkills(
                employeeId, userDetails.getUserId(), canManage(authentication)));
    }

    @PreAuthorize("hasAnyAuthority('AI_RECOMMENDATIONS_READ', 'AI_HR_TOOLS_USE')")
    @Operation(summary = "Recommend learning")
    @PostMapping("/employees/{employeeId}/recommend-learning")
    public ResponseEntity<AiSuggestionResponse> recommendLearning(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(aiService.recommendLearning(
                employeeId, userDetails.getUserId(), canManage(authentication)));
    }

    @PreAuthorize("hasAuthority('AI_HR_TOOLS_USE')")
    @Operation(summary = "Explain headcount analytics")
    @PostMapping("/analytics/headcount/explain")
    public ResponseEntity<AiSuggestionResponse> explainHeadcountAnalytics(Authentication authentication) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.explainHeadcountAnalytics(userId));
    }

    @PreAuthorize("hasAnyAuthority('AI_ASSISTANT_USE', 'AI_RECOMMENDATIONS_READ', 'AI_HR_TOOLS_USE')")
    @Operation(summary = "Get suggestion")
    @GetMapping("/suggestions/{suggestionId}")
    public ResponseEntity<AiSuggestionResponse> getSuggestion(
            Authentication authentication,
            @PathVariable UUID suggestionId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(aiService.getSuggestion(
                suggestionId, userDetails.getUserId(), canManage(authentication)));
    }

    @PreAuthorize("hasAnyAuthority('AI_ASSISTANT_USE', 'AI_RECOMMENDATIONS_READ', 'AI_HR_TOOLS_USE')")
    @Operation(summary = "Get my suggestions")
    @GetMapping("/my-suggestions")
    public ResponseEntity<Page<AiSuggestionResponse>> getMySuggestions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(aiService.getMySuggestions(userId, createPageable(page, size)));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> HR_TOOLS_AUTHORITY.equals(authority.getAuthority()));
    }

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        return PageRequest.of(page, size);
    }
}
