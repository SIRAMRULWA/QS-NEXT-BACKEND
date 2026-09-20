package za.co.qsnext.employeemanagement.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.ExpenseNotFoundException;
import za.co.qsnext.employeemanagement.expense.dto.ExpenseClaimResponse;
import za.co.qsnext.employeemanagement.integration.PayrollIntegrationProvider;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseCategoryRepository categoryRepository;
    @Mock
    private ExpenseClaimRepository claimRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private PayrollIntegrationProvider payrollIntegrationProvider;
    @Mock
    private AuditService auditService;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                categoryRepository, claimRepository, employeeRepository, documentRepository,
                notificationPublisher, payrollIntegrationProvider, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private ExpenseCategory categoryWithId(UUID id) {
        ExpenseCategory category = new ExpenseCategory("Travel", "Desc");
        setId(category, id);
        return category;
    }

    private ExpenseClaim claimWithId(UUID id, UUID employeeId, UUID categoryId) {
        ExpenseClaim claim = new ExpenseClaim(
                employeeId, categoryId, BigDecimal.valueOf(150), "ZAR", "Taxi",
                LocalDate.of(2026, 1, 10), null);
        setId(claim, id);
        return claim;
    }

    @Test
    void createCategory_rejectsADuplicateName() {
        when(categoryRepository.existsByName("Travel")).thenReturn(true);

        assertThatThrownBy(() -> expenseService.createCategory("Travel", "Desc"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createClaim_defaultsCurrency_whenNotProvided() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryWithId(categoryId)));
        when(claimRepository.save(any())).thenAnswer(invocation -> {
            ExpenseClaim claim = invocation.getArgument(0);
            setId(claim, UUID.randomUUID());
            return claim;
        });

        ExpenseClaimResponse response = expenseService.createClaim(
                employeeId, categoryId, BigDecimal.valueOf(100), null, "Lunch",
                LocalDate.of(2026, 1, 1), null, userId, false);

        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(response.status()).isEqualTo(ExpenseClaim.STATUS_DRAFT);
    }

    @Test
    void createClaim_throws_whenReceiptDocumentBelongsToAnotherEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();

        Document receipt = new Document(
                UUID.randomUUID(), "RECEIPT", "Receipt", null, UUID.randomUUID(), 1,
                "key", "r.pdf", "application/pdf", 10, null, UUID.randomUUID());

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(categoryWithId(categoryId)));
        when(documentRepository.findById(receiptId)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> expenseService.createClaim(
                employeeId, categoryId, BigDecimal.TEN, "ZAR", "Lunch",
                LocalDate.of(2026, 1, 1), receiptId, userId, false))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createClaim_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> expenseService.createClaim(
                employeeId, UUID.randomUUID(), BigDecimal.TEN, "ZAR", "Lunch",
                LocalDate.of(2026, 1, 1), null, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void submitClaim_transitionsDraftToSubmitted() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ExpenseClaim claim = claimWithId(claimId, employeeId, UUID.randomUUID());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        ExpenseClaimResponse response = expenseService.submitClaim(claimId, userId, false);

        assertThat(response.status()).isEqualTo(ExpenseClaim.STATUS_SUBMITTED);
    }

    @Test
    void submitClaim_throws_whenNotDraft() {
        UUID claimId = UUID.randomUUID();
        ExpenseClaim claim = claimWithId(claimId, UUID.randomUUID(), UUID.randomUUID());
        claim.submit();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> expenseService.submitClaim(claimId, UUID.randomUUID(), true))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void approveClaim_notifiesEmployeeAndCallsPayrollProvider() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        ExpenseClaim claim = claimWithId(claimId, employeeId, UUID.randomUUID());
        claim.submit();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        ExpenseClaimResponse response = expenseService.approveClaim(claimId, approverId);

        assertThat(response.status()).isEqualTo(ExpenseClaim.STATUS_APPROVED);
        verify(notificationPublisher).publish(eq(userId), eq(NotificationType.EXPENSE_APPROVED), any(), any());
        verify(payrollIntegrationProvider).notifyExpenseApproved(
                claimId, employeeId, claim.getAmount(), claim.getCurrency());
    }

    @Test
    void approveClaim_throws_whenNotSubmitted() {
        UUID claimId = UUID.randomUUID();
        ExpenseClaim claim = claimWithId(claimId, UUID.randomUUID(), UUID.randomUUID());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> expenseService.approveClaim(claimId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectClaim_recordsReasonAndNotifies() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExpenseClaim claim = claimWithId(claimId, employeeId, UUID.randomUUID());
        claim.submit();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        ExpenseClaimResponse response = expenseService.rejectClaim(claimId, UUID.randomUUID(), "Missing receipt");

        assertThat(response.status()).isEqualTo(ExpenseClaim.STATUS_REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Missing receipt");
        verify(notificationPublisher).publish(eq(userId), eq(NotificationType.EXPENSE_REJECTED), any(), any());
    }

    @Test
    void markReimbursed_throws_whenNotApproved() {
        UUID claimId = UUID.randomUUID();
        ExpenseClaim claim = claimWithId(claimId, UUID.randomUUID(), UUID.randomUUID());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> expenseService.markReimbursed(claimId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markReimbursed_notifiesAndCallsPayrollProvider() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExpenseClaim claim = claimWithId(claimId, employeeId, UUID.randomUUID());
        claim.submit();
        claim.approve(UUID.randomUUID());

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        ExpenseClaimResponse response = expenseService.markReimbursed(claimId);

        assertThat(response.status()).isEqualTo(ExpenseClaim.STATUS_REIMBURSED);
        verify(notificationPublisher).publish(eq(userId), eq(NotificationType.EXPENSE_REIMBURSED), any(), any());
        verify(payrollIntegrationProvider).notifyExpenseReimbursed(
                claimId, employeeId, claim.getAmount(), claim.getCurrency());
    }

    @Test
    void getCategorySummary_rejectsAnEndBeforeStart() {
        LocalDate from = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> expenseService.getCategorySummary(from, from.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findClaim_throws_whenClaimDoesNotExist() {
        UUID claimId = UUID.randomUUID();
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.approveClaim(claimId, UUID.randomUUID()))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
