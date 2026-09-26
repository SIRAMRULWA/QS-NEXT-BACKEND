package za.co.qsnext.employeemanagement.careers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.careers.dto.ApplyRequest;
import za.co.qsnext.employeemanagement.careers.dto.JobOpeningResponse;
import za.co.qsnext.employeemanagement.careers.dto.MyApplicationResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/careers")
@Tag(name = "Careers", description = "Open jobs and the applicant's own applications.")
public class CareersController {

    private final CareersService careersService;

    public CareersController(CareersService careersService) {
        this.careersService = careersService;
    }

    @PreAuthorize("hasAuthority('JOB_BOARD_READ')")
    @Operation(summary = "List open jobs")
    @GetMapping("/openings")
    public ResponseEntity<List<JobOpeningResponse>> getOpenings() {
        return ResponseEntity.ok(careersService.getOpenings());
    }

    @PreAuthorize("hasAuthority('APPLICATION_SELF')")
    @Operation(summary = "Apply for a job")
    @PostMapping("/openings/{postingId}/apply")
    public ResponseEntity<MyApplicationResponse> apply(
            Authentication authentication,
            @PathVariable UUID postingId,
            @Valid @RequestBody ApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careersService.apply(userId(authentication), postingId, request));
    }

    @PreAuthorize("hasAuthority('APPLICATION_SELF')")
    @Operation(summary = "My applications")
    @GetMapping("/my-applications")
    public ResponseEntity<List<MyApplicationResponse>> getMyApplications(Authentication authentication) {
        return ResponseEntity.ok(careersService.getMyApplications(userId(authentication)));
    }

    @PreAuthorize("hasAuthority('APPLICATION_SELF')")
    @Operation(summary = "Withdraw my application")
    @PatchMapping("/my-applications/{applicationId}/withdraw")
    public ResponseEntity<MyApplicationResponse> withdraw(
            Authentication authentication,
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(careersService.withdraw(userId(authentication), applicationId));
    }

    private UUID userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUserId();
    }
}
