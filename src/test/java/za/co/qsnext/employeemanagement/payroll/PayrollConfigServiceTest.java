package za.co.qsnext.employeemanagement.payroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.PayrollNotFoundException;
import za.co.qsnext.employeemanagement.payroll.dto.EmployeePayrollProfileResponse;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollConfigServiceTest {

    @Mock
    private TaxConfigurationRepository taxConfigurationRepository;
    @Mock
    private TaxBracketRepository taxBracketRepository;
    @Mock
    private EmployeePayrollProfileRepository payrollProfileRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AuditService auditService;

    private PayrollConfigService payrollConfigService;

    @BeforeEach
    void setUp() {
        payrollConfigService = new PayrollConfigService(
                taxConfigurationRepository, taxBracketRepository, payrollProfileRepository,
                employeeRepository, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private TaxBracket bracket(UUID taxConfigId, String min, String max, String rate) {
        return new TaxBracket(
                taxConfigId, new BigDecimal(min), max == null ? null : new BigDecimal(max),
                new BigDecimal(rate), LocalDate.of(2026, 1, 1), null);
    }

    @Test
    void createTaxConfiguration_rejectsADuplicateName() {
        when(taxConfigurationRepository.existsByName("PAYE")).thenReturn(true);

        assertThatThrownBy(() -> payrollConfigService.createTaxConfiguration(
                "PAYE", "Desc", TaxConfiguration.LINE_ITEM_TYPE_DEDUCTION))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void addTaxBracket_rejectsAMaxAmountNotGreaterThanMin() {
        UUID taxConfigId = UUID.randomUUID();
        when(taxConfigurationRepository.existsById(taxConfigId)).thenReturn(true);

        assertThatThrownBy(() -> payrollConfigService.addTaxBracket(
                taxConfigId, BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                BigDecimal.TEN, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addTaxBracket_throws_whenTaxConfigurationDoesNotExist() {
        UUID taxConfigId = UUID.randomUUID();
        when(taxConfigurationRepository.existsById(taxConfigId)).thenReturn(false);

        assertThatThrownBy(() -> payrollConfigService.addTaxBracket(
                taxConfigId, BigDecimal.ZERO, null, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(PayrollNotFoundException.class);
    }

    @Test
    void calculateTax_appliesProgressiveBrackets() {
        UUID taxConfigId = UUID.randomUUID();

        // 0 - 10000 at 10%, 10000+ at 20%
        List<TaxBracket> brackets = List.of(
                bracket(taxConfigId, "0", "10000", "10"),
                bracket(taxConfigId, "10000", null, "20")
        );

        when(taxBracketRepository.findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigId)).thenReturn(brackets);

        // 15000 taxable: 10000*10% + 5000*20% = 1000 + 1000 = 2000
        BigDecimal tax = payrollConfigService.calculateTax(
                taxConfigId, BigDecimal.valueOf(15000), LocalDate.of(2026, 3, 1));

        assertThat(tax).isEqualByComparingTo("2000.00");
    }

    @Test
    void calculateTax_onlyAppliesTheFirstBracket_whenIncomeIsBelowTheSecond() {
        UUID taxConfigId = UUID.randomUUID();

        List<TaxBracket> brackets = List.of(
                bracket(taxConfigId, "0", "10000", "10"),
                bracket(taxConfigId, "10000", null, "20")
        );

        when(taxBracketRepository.findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigId)).thenReturn(brackets);

        // 5000 taxable: 5000*10% = 500
        BigDecimal tax = payrollConfigService.calculateTax(
                taxConfigId, BigDecimal.valueOf(5000), LocalDate.of(2026, 3, 1));

        assertThat(tax).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateTax_treatsASingleBracketAsAFlatRateWithCeiling() {
        UUID taxConfigId = UUID.randomUUID();

        // UIF-style: 1% up to a 5000 ceiling
        List<TaxBracket> brackets = List.of(bracket(taxConfigId, "0", "5000", "1"));

        when(taxBracketRepository.findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigId)).thenReturn(brackets);

        BigDecimal tax = payrollConfigService.calculateTax(
                taxConfigId, BigDecimal.valueOf(20000), LocalDate.of(2026, 3, 1));

        assertThat(tax).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateTax_ignoresBracketsNotEffectiveOnTheGivenDate() {
        UUID taxConfigId = UUID.randomUUID();

        TaxBracket expired = new TaxBracket(
                taxConfigId, BigDecimal.ZERO, null, BigDecimal.TEN,
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));

        when(taxBracketRepository.findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigId))
                .thenReturn(List.of(expired));

        BigDecimal tax = payrollConfigService.calculateTax(
                taxConfigId, BigDecimal.valueOf(15000), LocalDate.of(2026, 3, 1));

        assertThat(tax).isEqualByComparingTo("0.00");
    }

    @Test
    void upsertPayrollProfile_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> payrollConfigService.upsertPayrollProfile(
                employeeId, BigDecimal.valueOf(50000), EmployeePayrollProfile.FREQUENCY_MONTHLY,
                null, null, null, null))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void upsertPayrollProfile_updatesAnExistingProfile() {
        UUID employeeId = UUID.randomUUID();
        EmployeePayrollProfile existing = new EmployeePayrollProfile(
                employeeId, BigDecimal.valueOf(40000), EmployeePayrollProfile.FREQUENCY_MONTHLY,
                null, null, null, null);
        setId(existing, UUID.randomUUID());

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(payrollProfileRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(existing));
        when(payrollProfileRepository.save(existing)).thenReturn(existing);

        EmployeePayrollProfileResponse response = payrollConfigService.upsertPayrollProfile(
                employeeId, BigDecimal.valueOf(55000), EmployeePayrollProfile.FREQUENCY_MONTHLY,
                null, null, null, null);

        assertThat(response.baseSalary()).isEqualByComparingTo("55000");
    }

    @Test
    void getPayrollProfile_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> payrollConfigService.getPayrollProfile(employeeId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getPayrollProfile_throws_whenNoProfileExists() {
        UUID employeeId = UUID.randomUUID();
        when(payrollProfileRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollConfigService.getPayrollProfile(employeeId, UUID.randomUUID(), true))
                .isInstanceOf(PayrollNotFoundException.class);
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
