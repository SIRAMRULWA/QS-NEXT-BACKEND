package za.co.qsnext.employeemanagement.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalPayrollIntegrationProviderTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;
    @Mock
    private AuditService auditService;

    private InternalPayrollIntegrationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InternalPayrollIntegrationProvider(integrationConfigRepository, auditService);
    }

    @Test
    void notifyExpenseApproved_alwaysRecordsAnAuditEntry_regardlessOfConfigState() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(integrationConfigRepository.findByType(IntegrationConfig.TYPE_PAYROLL))
                .thenReturn(Optional.empty());

        provider.notifyExpenseApproved(claimId, employeeId, BigDecimal.TEN, "ZAR");

        verify(auditService).log(
                "EXPENSE_QUEUED_FOR_PAYROLL", "ExpenseClaim", claimId, AuditService.RESULT_SUCCESS
        );
    }

    @Test
    void notifyExpenseReimbursed_recordsAnAuditEntry_whenPayrollIntegrationIsEnabled() {
        UUID claimId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        IntegrationConfig config = new IntegrationConfig(IntegrationConfig.TYPE_PAYROLL, "acme-payroll", true);
        setId(config, UUID.randomUUID());

        when(integrationConfigRepository.findByType(IntegrationConfig.TYPE_PAYROLL))
                .thenReturn(Optional.of(config));

        provider.notifyExpenseReimbursed(claimId, employeeId, BigDecimal.TEN, "ZAR");

        verify(auditService).log(
                "EXPENSE_REIMBURSEMENT_CONFIRMED", "ExpenseClaim", claimId, AuditService.RESULT_SUCCESS
        );
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
