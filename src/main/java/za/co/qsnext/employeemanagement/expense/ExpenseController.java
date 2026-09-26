package za.co.qsnext.employeemanagement.expense;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.expense.dto.CreateExpenseCategoryRequest;
import za.co.qsnext.employeemanagement.expense.dto.CreateExpenseClaimRequest;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseCategoryResponse;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseCategorySummaryResponse;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseClaimResponse;
import za.co.qsnext.employeemanagement.expense.dto.RejectExpenseClaimRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Expenses", description = "Expense categories, claims and approval workflow.")
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private static final String MANAGE_AUTHORITY = "EXPENSE_MANAGE";

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @Operation(summary = "Create category")
    @PostMapping("/categories")
    public ResponseEntity<ExpenseCategoryResponse> createCategory(
            @Valid @RequestBody CreateExpenseCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                expenseService.createCategory(request.name(), request.description())
        );
    }

    @PreAuthorize("hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_READ')")
    @Operation(summary = "Get active categories")
    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(expenseService.getActiveCategories());
    }

    @PreAuthorize("hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_READ')")
    @Operation(summary = "Create claim")
    @PostMapping("/employees/{employeeId}/claims")
    public ResponseEntity<ExpenseClaimResponse> createClaim(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateExpenseClaimRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        ExpenseClaimResponse response = expenseService.createClaim(
                employeeId, request.categoryId(), request.amount(), request.currency(), request.description(),
                request.expenseDate(), request.receiptDocumentId(), userDetails.getUserId(), canManage(authentication)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_READ')")
    @Operation(summary = "Submit claim")
    @PatchMapping("/claims/{claimId}/submit")
    public ResponseEntity<ExpenseClaimResponse> submitClaim(
            Authentication authentication,
            @PathVariable UUID claimId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(expenseService.submitClaim(
                claimId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_READ')")
    @Operation(summary = "Get claims for employee")
    @GetMapping("/employees/{employeeId}/claims")
    public ResponseEntity<List<ExpenseClaimResponse>> getClaimsForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(expenseService.getClaimsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    @Operation(summary = "Get my claims")
    @GetMapping("/my-claims")
    public ResponseEntity<List<ExpenseClaimResponse>> getMyClaims(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(expenseService.getMyClaims(userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @Operation(summary = "Get pending approvals")
    @GetMapping("/pending-approvals")
    public ResponseEntity<Page<ExpenseClaimResponse>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));

        return ResponseEntity.ok(expenseService.getPendingApprovals(pageable));
    }

    @PreAuthorize(
            "hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_APPROVE') and " +
                    "@expenseAuthorizationService.canApproveClaim(#claimId, authentication)"
    )
    @Operation(summary = "Approve claim")
    @PatchMapping("/claims/{claimId}/approve")
    public ResponseEntity<ExpenseClaimResponse> approveClaim(
            Authentication authentication,
            @PathVariable UUID claimId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(expenseService.approveClaim(claimId, userDetails.getUserId()));
    }

    @PreAuthorize(
            "hasAnyAuthority('EXPENSE_MANAGE', 'EXPENSE_APPROVE') and " +
                    "@expenseAuthorizationService.canApproveClaim(#claimId, authentication)"
    )
    @Operation(summary = "Reject claim")
    @PatchMapping("/claims/{claimId}/reject")
    public ResponseEntity<ExpenseClaimResponse> rejectClaim(
            Authentication authentication,
            @PathVariable UUID claimId,
            @Valid @RequestBody RejectExpenseClaimRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(expenseService.rejectClaim(claimId, userDetails.getUserId(), request.reason()));
    }

    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @Operation(summary = "Mark reimbursed")
    @PatchMapping("/claims/{claimId}/reimburse")
    public ResponseEntity<ExpenseClaimResponse> markReimbursed(@PathVariable UUID claimId) {
        return ResponseEntity.ok(expenseService.markReimbursed(claimId));
    }

    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @Operation(summary = "Get category summary")
    @GetMapping("/reports/by-category")
    public ResponseEntity<List<ExpenseCategorySummaryResponse>> getCategorySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(expenseService.getCategorySummary(from, to));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
