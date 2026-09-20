package za.co.qsnext.employeemanagement.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import za.co.qsnext.employeemanagement.audit.AuditService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default {@link PayrollIntegrationProvider}: records an audit entry and
 * logs the event. This does NOT talk to any actual payroll system - see
 * the interface javadoc. Whether the PAYROLL {@link IntegrationConfig}
 * is enabled only changes the log level, not whether the audit entry is
 * written; the approval itself is always worth recording regardless of
 * whether payroll integration is switched on.
 */
@Component
public class InternalPayrollIntegrationProvider implements PayrollIntegrationProvider {

    private static final Logger log = LoggerFactory.getLogger(InternalPayrollIntegrationProvider.class);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final AuditService auditService;

    public InternalPayrollIntegrationProvider(
            IntegrationConfigRepository integrationConfigRepository,
            AuditService auditService
    ) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.auditService = auditService;
    }

    @Override
    public void notifyExpenseApproved(UUID expenseClaimId, UUID employeeId, BigDecimal amount, String currency) {
        record("EXPENSE_QUEUED_FOR_PAYROLL", expenseClaimId, employeeId, amount, currency);
    }

    @Override
    public void notifyExpenseReimbursed(UUID expenseClaimId, UUID employeeId, BigDecimal amount, String currency) {
        record("EXPENSE_REIMBURSEMENT_CONFIRMED", expenseClaimId, employeeId, amount, currency);
    }

    private void record(String action, UUID expenseClaimId, UUID employeeId, BigDecimal amount, String currency) {

        boolean payrollEnabled = integrationConfigRepository.findByType(IntegrationConfig.TYPE_PAYROLL)
                .map(IntegrationConfig::isEnabled)
                .orElse(false);

        if (payrollEnabled) {
            log.info(
                    "{}: expense {} for employee {} ({} {}) would be sent to the configured payroll provider",
                    action, expenseClaimId, employeeId, amount, currency
            );
        } else {
            log.debug(
                    "{}: expense {} for employee {} ({} {}) - no payroll provider configured, audit only",
                    action, expenseClaimId, employeeId, amount, currency
            );
        }

        auditService.log(action, "ExpenseClaim", expenseClaimId, AuditService.RESULT_SUCCESS);
    }
}
