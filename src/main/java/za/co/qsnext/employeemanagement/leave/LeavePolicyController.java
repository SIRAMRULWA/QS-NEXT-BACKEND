package za.co.qsnext.employeemanagement.leave;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.leave.dto.LeavePolicyRequest;
import za.co.qsnext.employeemanagement.leave.dto.LeavePolicyResponse;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leave/policies")
@Tag(name = "Leave policies", description = "Yearly entitlements, accrual and carry-over per leave type.")
@PreAuthorize("hasAuthority('LEAVE_ALLOCATE')")
public class LeavePolicyController {

    private final LeaveAccrualService accrualService;

    public LeavePolicyController(LeaveAccrualService accrualService) {
        this.accrualService = accrualService;
    }

    @Operation(summary = "List policies")
    @GetMapping
    public ResponseEntity<List<LeavePolicyResponse>> getPolicies() {
        return ResponseEntity.ok(accrualService.getPolicies());
    }

    @Operation(summary = "Create or update a policy")
    @PutMapping
    public ResponseEntity<LeavePolicyResponse> savePolicy(@Valid @RequestBody LeavePolicyRequest request) {
        return ResponseEntity.ok(accrualService.savePolicy(request));
    }

    @Operation(summary = "Run this month's accrual now")
    @PostMapping("/accrue")
    public ResponseEntity<Map<String, Integer>> accrueNow() {
        return ResponseEntity.ok(Map.of("balancesCredited", accrualService.accrue(YearMonth.now())));
    }
}
