package za.co.qsnext.employeemanagement.payroll;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.payroll.dto.CreateTaxBracketRequest;
import za.co.qsnext.employeemanagement.payroll.dto.CreateTaxConfigurationRequest;
import za.co.qsnext.employeemanagement.payroll.dto.EmployeePayrollProfileResponse;
import za.co.qsnext.employeemanagement.payroll.dto.TaxBracketResponse;
import za.co.qsnext.employeemanagement.payroll.dto.TaxConfigurationResponse;
import za.co.qsnext.employeemanagement.payroll.dto.UpsertPayrollProfileRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll/config")
public class PayrollConfigController {

    private static final String MANAGE_AUTHORITY = "PAYROLL_MANAGE";

    private final PayrollConfigService payrollConfigService;

    public PayrollConfigController(PayrollConfigService payrollConfigService) {
        this.payrollConfigService = payrollConfigService;
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/tax-configurations")
    public ResponseEntity<TaxConfigurationResponse> createTaxConfiguration(
            @Valid @RequestBody CreateTaxConfigurationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollConfigService.createTaxConfiguration(
                request.name(), request.description(), request.lineItemType()
        ));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @GetMapping("/tax-configurations")
    public ResponseEntity<List<TaxConfigurationResponse>> getActiveTaxConfigurations() {
        return ResponseEntity.ok(payrollConfigService.getActiveTaxConfigurations());
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/tax-configurations/{taxConfigurationId}/brackets")
    public ResponseEntity<TaxBracketResponse> addTaxBracket(
            @PathVariable UUID taxConfigurationId,
            @Valid @RequestBody CreateTaxBracketRequest request
    ) {
        TaxBracketResponse response = payrollConfigService.addTaxBracket(
                taxConfigurationId, request.minAmount(), request.maxAmount(),
                request.ratePercent(), request.effectiveFrom(), request.effectiveTo()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @GetMapping("/tax-configurations/{taxConfigurationId}/brackets")
    public ResponseEntity<List<TaxBracketResponse>> getTaxBrackets(@PathVariable UUID taxConfigurationId) {
        return ResponseEntity.ok(payrollConfigService.getTaxBrackets(taxConfigurationId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PutMapping("/employees/{employeeId}/profile")
    public ResponseEntity<EmployeePayrollProfileResponse> upsertPayrollProfile(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpsertPayrollProfileRequest request
    ) {
        EmployeePayrollProfileResponse response = payrollConfigService.upsertPayrollProfile(
                employeeId, request.baseSalary(), request.payFrequency(), request.standardHoursPerPeriod(),
                request.overtimeHourlyRate(), request.bankAccountReference(), request.taxNumber()
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('PAYROLL_MANAGE', 'PAYROLL_READ')")
    @GetMapping("/employees/{employeeId}/profile")
    public ResponseEntity<EmployeePayrollProfileResponse> getPayrollProfile(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(payrollConfigService.getPayrollProfile(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PatchMapping("/employees/{employeeId}/profile/deactivate")
    public ResponseEntity<EmployeePayrollProfileResponse> deactivateProfile(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(payrollConfigService.deactivateProfile(employeeId));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
