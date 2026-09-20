package za.co.qsnext.employeemanagement.compliance;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.compliance.dto.CompleteComplianceRecordRequest;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRecordResponse;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRequirementResponse;
import za.co.qsnext.employeemanagement.compliance.dto.ComplianceRequirementSummaryResponse;
import za.co.qsnext.employeemanagement.compliance.dto.CreateComplianceRequirementRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Compliance", description = "Compliance requirements and employee compliance records.")
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private static final String MANAGE_AUTHORITY = "COMPLIANCE_MANAGE";

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PreAuthorize("hasAuthority('COMPLIANCE_MANAGE')")
    @Operation(summary = "Create requirement")
    @PostMapping("/requirements")
    public ResponseEntity<ComplianceRequirementResponse> createRequirement(
            @Valid @RequestBody CreateComplianceRequirementRequest request
    ) {
        ComplianceRequirementResponse response = complianceService.createRequirement(
                request.name(), request.description(), request.category(),
                request.mandatory(), request.validityPeriodDays()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('COMPLIANCE_MANAGE', 'COMPLIANCE_READ')")
    @Operation(summary = "Get active requirements")
    @GetMapping("/requirements")
    public ResponseEntity<List<ComplianceRequirementResponse>> getActiveRequirements() {
        return ResponseEntity.ok(complianceService.getActiveRequirements());
    }

    @PreAuthorize("hasAuthority('COMPLIANCE_MANAGE')")
    @Operation(summary = "Assign to employee")
    @PostMapping("/employees/{employeeId}/records")
    public ResponseEntity<ComplianceRecordResponse> assignToEmployee(
            @PathVariable UUID employeeId,
            @RequestParam UUID requirementId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                complianceService.assignToEmployee(employeeId, requirementId)
        );
    }

    @PreAuthorize("hasAnyAuthority('COMPLIANCE_MANAGE', 'COMPLIANCE_READ')")
    @Operation(summary = "Get records for employee")
    @GetMapping("/employees/{employeeId}/records")
    public ResponseEntity<List<ComplianceRecordResponse>> getRecordsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(complianceService.getRecordsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('COMPLIANCE_READ')")
    @Operation(summary = "Get my records")
    @GetMapping("/my-records")
    public ResponseEntity<List<ComplianceRecordResponse>> getMyRecords(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(complianceService.getMyRecords(userDetails.getUserId()));
    }

    @PreAuthorize("hasAnyAuthority('COMPLIANCE_MANAGE', 'COMPLIANCE_READ')")
    @Operation(summary = "Complete record")
    @PatchMapping("/records/{recordId}/complete")
    public ResponseEntity<ComplianceRecordResponse> completeRecord(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody(required = false) CompleteComplianceRecordRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID evidenceDocumentId = request == null ? null : request.evidenceDocumentId();
        String notes = request == null ? null : request.notes();

        return ResponseEntity.ok(complianceService.completeRecord(
                recordId, userDetails.getUserId(), canManage(authentication), evidenceDocumentId, notes
        ));
    }

    @PreAuthorize("hasAuthority('COMPLIANCE_MANAGE')")
    @Operation(summary = "Get expiring records")
    @GetMapping("/expiring")
    public ResponseEntity<List<ComplianceRecordResponse>> getExpiringRecords(
            @RequestParam(defaultValue = "30") int withinDays
    ) {
        return ResponseEntity.ok(complianceService.getExpiringRecords(withinDays));
    }

    @PreAuthorize("hasAuthority('COMPLIANCE_MANAGE')")
    @Operation(summary = "Get requirement summary")
    @GetMapping("/requirements/{requirementId}/summary")
    public ResponseEntity<ComplianceRequirementSummaryResponse> getRequirementSummary(
            @PathVariable UUID requirementId
    ) {
        return ResponseEntity.ok(complianceService.getRequirementSummary(requirementId));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
