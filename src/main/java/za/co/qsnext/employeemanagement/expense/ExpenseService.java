package za.co.qsnext.employeemanagement.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.ExpenseNotFoundException;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseCategoryResponse;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseCategorySummaryResponse;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseClaimResponse;
import za.co.qsnext.employeemanagement.integration.PayrollIntegrationProvider;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private static final String DEFAULT_CURRENCY = "ZAR";
    private static final String ENTITY_TYPE = "ExpenseClaim";

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseClaimRepository claimRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final NotificationPublisher notificationPublisher;
    private final PayrollIntegrationProvider payrollIntegrationProvider;
    private final AuditService auditService;

    public ExpenseService(
            ExpenseCategoryRepository categoryRepository,
            ExpenseClaimRepository claimRepository,
            EmployeeRepository employeeRepository,
            DocumentRepository documentRepository,
            NotificationPublisher notificationPublisher,
            PayrollIntegrationProvider payrollIntegrationProvider,
            AuditService auditService
    ) {
        this.categoryRepository = categoryRepository;
        this.claimRepository = claimRepository;
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.notificationPublisher = notificationPublisher;
        this.payrollIntegrationProvider = payrollIntegrationProvider;
        this.auditService = auditService;
    }

    @Transactional
    public ExpenseCategoryResponse createCategory(String name, String description) {

        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Expense category already exists: " + name);
        }

        return ExpenseCategoryResponse.from(categoryRepository.save(new ExpenseCategory(name, description)));
    }

    public List<ExpenseCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream().map(ExpenseCategoryResponse::from).toList();
    }

    @Transactional
    public ExpenseClaimResponse createClaim(
            UUID employeeId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate expenseDate,
            UUID receiptDocumentId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeExpenses(employeeId, requesterUserId, requesterCanManage);

        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        }

        ExpenseCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense category not found: " + categoryId));

        if (!category.isActive()) {
            throw new BusinessRuleException("Expense category is no longer active: " + category.getName());
        }

        if (receiptDocumentId != null) {
            Document receipt = documentRepository.findById(receiptDocumentId)
                    .orElseThrow(() -> new BusinessRuleException("Receipt document not found: " + receiptDocumentId));

            if (!receipt.getEmployeeId().equals(employeeId)) {
                throw new BusinessRuleException("Receipt document does not belong to this employee");
            }
        }

        String resolvedCurrency = (currency == null || currency.isBlank()) ? DEFAULT_CURRENCY : currency;

        ExpenseClaim claim = claimRepository.save(new ExpenseClaim(
                employeeId, categoryId, amount, resolvedCurrency, description, expenseDate, receiptDocumentId
        ));

        auditService.log("EXPENSE_CLAIM_CREATED", ENTITY_TYPE, claim.getId(), AuditService.RESULT_SUCCESS);

        return ExpenseClaimResponse.from(claim);
    }

    @Transactional
    public ExpenseClaimResponse submitClaim(UUID claimId, UUID requesterUserId, boolean requesterCanManage) {

        ExpenseClaim claim = findClaimOrThrow(claimId);
        assertCanAccessEmployeeExpenses(claim.getEmployeeId(), requesterUserId, requesterCanManage);

        if (!claim.isDraft()) {
            throw new BusinessRuleException("Only a draft expense claim can be submitted");
        }

        claim.submit();

        auditService.log("EXPENSE_CLAIM_SUBMITTED", ENTITY_TYPE, claimId, AuditService.RESULT_SUCCESS);

        return ExpenseClaimResponse.from(claim);
    }

    public List<ExpenseClaimResponse> getClaimsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeExpenses(employeeId, requesterUserId, requesterCanManage);

        return claimRepository.findByEmployeeIdOrderByExpenseDateDesc(employeeId).stream()
                .map(ExpenseClaimResponse::from)
                .toList();
    }

    public List<ExpenseClaimResponse> getMyClaims(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return claimRepository.findByEmployeeIdOrderByExpenseDateDesc(employee.getId()).stream()
                .map(ExpenseClaimResponse::from)
                .toList();
    }

    public Page<ExpenseClaimResponse> getPendingApprovals(Pageable pageable) {
        return claimRepository.findByStatus(ExpenseClaim.STATUS_SUBMITTED, pageable).map(ExpenseClaimResponse::from);
    }

    @Transactional
    public ExpenseClaimResponse approveClaim(UUID claimId, UUID approvedBy) {

        ExpenseClaim claim = findClaimOrThrow(claimId);

        if (!claim.isSubmitted()) {
            throw new BusinessRuleException("Only a submitted expense claim can be approved");
        }

        claim.approve(approvedBy);

        Employee employee = employeeRepository.findById(claim.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + claim.getEmployeeId()));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.EXPENSE_APPROVED,
                "Expense claim approved",
                "Your expense claim for " + claim.getAmount() + " " + claim.getCurrency() + " was approved."
        );

        payrollIntegrationProvider.notifyExpenseApproved(
                claim.getId(), claim.getEmployeeId(), claim.getAmount(), claim.getCurrency()
        );

        auditService.log("EXPENSE_CLAIM_APPROVED", ENTITY_TYPE, claimId, AuditService.RESULT_SUCCESS);

        return ExpenseClaimResponse.from(claim);
    }

    @Transactional
    public ExpenseClaimResponse rejectClaim(UUID claimId, UUID rejectedBy, String reason) {

        ExpenseClaim claim = findClaimOrThrow(claimId);

        if (!claim.isSubmitted()) {
            throw new BusinessRuleException("Only a submitted expense claim can be rejected");
        }

        claim.reject(rejectedBy, reason);

        Employee employee = employeeRepository.findById(claim.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + claim.getEmployeeId()));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.EXPENSE_REJECTED,
                "Expense claim rejected",
                "Your expense claim for " + claim.getAmount() + " " + claim.getCurrency() + " was rejected: " + reason
        );

        auditService.log("EXPENSE_CLAIM_REJECTED", ENTITY_TYPE, claimId, AuditService.RESULT_SUCCESS);

        return ExpenseClaimResponse.from(claim);
    }

    @Transactional
    public ExpenseClaimResponse markReimbursed(UUID claimId) {

        ExpenseClaim claim = findClaimOrThrow(claimId);

        if (!claim.isApproved()) {
            throw new BusinessRuleException("Only an approved expense claim can be marked as reimbursed");
        }

        claim.markReimbursed();

        Employee employee = employeeRepository.findById(claim.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + claim.getEmployeeId()));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.EXPENSE_REIMBURSED,
                "Expense claim reimbursed",
                "Your expense claim for " + claim.getAmount() + " " + claim.getCurrency() + " has been reimbursed."
        );

        payrollIntegrationProvider.notifyExpenseReimbursed(
                claim.getId(), claim.getEmployeeId(), claim.getAmount(), claim.getCurrency()
        );

        auditService.log("EXPENSE_CLAIM_REIMBURSED", ENTITY_TYPE, claimId, AuditService.RESULT_SUCCESS);

        return ExpenseClaimResponse.from(claim);
    }

    public List<ExpenseCategorySummaryResponse> getCategorySummary(LocalDate from, LocalDate to) {

        if (to.isBefore(from)) {
            throw new BusinessRuleException("Range end cannot be before range start");
        }

        return claimRepository.summarizeByCategory(from, to).stream()
                .map(summary -> new ExpenseCategorySummaryResponse(
                        summary.getCategoryId(), summary.getTotalAmount(), summary.getClaimCount()
                ))
                .toList();
    }

    private ExpenseClaim findClaimOrThrow(UUID claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense claim not found: " + claimId));
    }

    private void assertCanAccessEmployeeExpenses(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to access this employee's expense claims");
        }
    }
}
