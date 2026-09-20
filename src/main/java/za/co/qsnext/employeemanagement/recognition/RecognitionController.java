package za.co.qsnext.employeemanagement.recognition;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.recognition.dto.CreateRecognitionTypeRequest;
import za.co.qsnext.employeemanagement.recognition.dto.GiveRecognitionRequest;
import za.co.qsnext.employeemanagement.recognition.dto.LeaderboardEntryResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionTypeResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Recognition", description = "Peer recognition and points.")
@RestController
@RequestMapping("/api/v1/recognition")
public class RecognitionController {

    private static final String MANAGE_AUTHORITY = "RECOGNITION_MANAGE";

    private final RecognitionService recognitionService;

    public RecognitionController(RecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @PreAuthorize("hasAuthority('RECOGNITION_MANAGE')")
    @Operation(summary = "Create type")
    @PostMapping("/types")
    public ResponseEntity<RecognitionTypeResponse> createType(
            @Valid @RequestBody CreateRecognitionTypeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                recognitionService.createType(request.name(), request.description(), request.pointValue())
        );
    }

    @PreAuthorize("hasAnyAuthority('RECOGNITION_MANAGE', 'RECOGNITION_GIVE')")
    @Operation(summary = "Get active types")
    @GetMapping("/types")
    public ResponseEntity<List<RecognitionTypeResponse>> getActiveTypes() {
        return ResponseEntity.ok(recognitionService.getActiveTypes());
    }

    @PreAuthorize("hasAuthority('RECOGNITION_GIVE')")
    @Operation(summary = "Give recognition")
    @PostMapping
    public ResponseEntity<RecognitionResponse> giveRecognition(
            Authentication authentication,
            @Valid @RequestBody GiveRecognitionRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        RecognitionResponse response = recognitionService.giveRecognition(
                request.typeId(), userDetails.getUserId(), request.givenToEmployeeId(),
                request.message(), request.visibility()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('RECOGNITION_MANAGE', 'RECOGNITION_GIVE')")
    @Operation(summary = "Get received by employee")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<List<RecognitionResponse>> getReceivedByEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(recognitionService.getReceivedByEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('RECOGNITION_GIVE')")
    @Operation(summary = "Get given by me")
    @GetMapping("/given")
    public ResponseEntity<List<RecognitionResponse>> getGivenByMe(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(recognitionService.getGivenByUser(userDetails.getUserId()));
    }

    @PreAuthorize("hasAnyAuthority('RECOGNITION_MANAGE', 'RECOGNITION_GIVE')")
    @Operation(summary = "Get points total")
    @GetMapping("/employees/{employeeId}/points-total")
    public ResponseEntity<Long> getPointsTotal(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(recognitionService.getPointsTotalForEmployee(employeeId));
    }

    @PreAuthorize("hasAnyAuthority('RECOGNITION_MANAGE', 'RECOGNITION_GIVE')")
    @Operation(summary = "Get leaderboard")
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(recognitionService.getLeaderboard(limit));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
