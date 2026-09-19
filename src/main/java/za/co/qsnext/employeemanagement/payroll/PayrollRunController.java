package za.co.qsnext.employeemanagement.payroll;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.payroll.dto.AddLineItemRequest;
import za.co.qsnext.employeemanagement.payroll.dto.CreatePayPeriodRequest;
import za.co.qsnext.employeemanagement.payroll.dto.PayPeriodResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunEntryResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayslipResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Payroll Runs", description = "Pay periods, payroll runs, line items and payslips.")
@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollRunController {

    private static final String MANAGE_AUTHORITY = "PAYROLL_MANAGE";

    private final PayrollRunService payrollRunService;

    public PayrollRunController(PayrollRunService payrollRunService) {
        this.payrollRunService = payrollRunService;
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Create pay period")
    @PostMapping("/pay-periods")
    public ResponseEntity<PayPeriodResponse> createPayPeriod(@Valid @RequestBody CreatePayPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollRunService.createPayPeriod(
                request.name(), request.startDate(), request.endDate(), request.payDate()
        ));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Get all pay periods")
    @GetMapping("/pay-periods")
    public ResponseEntity<List<PayPeriodResponse>> getAllPayPeriods() {
        return ResponseEntity.ok(payrollRunService.getAllPayPeriods());
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Create run")
    @PostMapping("/runs")
    public ResponseEntity<PayrollRunResponse> createRun(
            Authentication authentication,
            @RequestParam UUID payPeriodId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                payrollRunService.createRun(payPeriodId, userDetails.getUserId())
        );
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Get run")
    @GetMapping("/runs/{runId}")
    public ResponseEntity<PayrollRunResponse> getRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(payrollRunService.getRun(runId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Get run entries")
    @GetMapping("/runs/{runId}/entries")
    public ResponseEntity<List<PayrollRunEntryResponse>> getRunEntries(@PathVariable UUID runId) {
        return ResponseEntity.ok(payrollRunService.getRunEntries(runId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Add line item")
    @PostMapping("/entries/{entryId}/line-items")
    public ResponseEntity<PayrollRunEntryResponse> addLineItem(
            @PathVariable UUID entryId,
            @Valid @RequestBody AddLineItemRequest request
    ) {
        PayrollRunEntryResponse response = payrollRunService.addLineItem(
                entryId, request.type(), request.code(), request.description(), request.amount()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Approve run")
    @PatchMapping("/runs/{runId}/approve")
    public ResponseEntity<PayrollRunResponse> approveRun(
            Authentication authentication,
            @PathVariable UUID runId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(payrollRunService.approveRun(runId, userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @Operation(summary = "Mark run paid")
    @PatchMapping("/runs/{runId}/pay")
    public ResponseEntity<PayrollRunResponse> markRunPaid(@PathVariable UUID runId) {
        return ResponseEntity.ok(payrollRunService.markRunPaid(runId));
    }

    @PreAuthorize("hasAnyAuthority('PAYROLL_MANAGE', 'PAYROLL_READ')")
    @Operation(summary = "Get payslip")
    @GetMapping("/entries/{entryId}/payslip")
    public ResponseEntity<PayslipResponse> getPayslip(
            Authentication authentication,
            @PathVariable UUID entryId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(payrollRunService.getPayslip(
                entryId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('PAYROLL_MANAGE', 'PAYROLL_READ')")
    @Operation(summary = "Get payslips for employee")
    @GetMapping("/employees/{employeeId}/payslips")
    public ResponseEntity<List<PayrollRunEntryResponse>> getPayslipsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(payrollRunService.getPayslipsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    @Operation(summary = "Get my payslips")
    @GetMapping("/my-payslips")
    public ResponseEntity<List<PayrollRunEntryResponse>> getMyPayslips(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(payrollRunService.getMyPayslips(userDetails.getUserId()));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
