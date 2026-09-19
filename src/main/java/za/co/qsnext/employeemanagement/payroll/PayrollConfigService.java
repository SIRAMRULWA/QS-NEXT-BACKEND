package za.co.qsnext.employeemanagement.payroll;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.PayrollNotFoundException;
import za.co.qsnext.employeemanagement.payroll.dto.EmployeePayrollProfileResponse;
import za.co.qsnext.employeemanagement.payroll.dto.TaxBracketResponse;
import za.co.qsnext.employeemanagement.payroll.dto.TaxConfigurationResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Owns Payroll's configuration surface: tax configurations/brackets and
 * per-employee payroll profiles. Deliberately contains no actual SARS
 * (or any other jurisdiction's) rates - see {@link TaxConfiguration}
 * and {@link TaxBracket}'s javadoc.
 */
@Service
@Transactional(readOnly = true)
public class PayrollConfigService {

    private final TaxConfigurationRepository taxConfigurationRepository;
    private final TaxBracketRepository taxBracketRepository;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public PayrollConfigService(
            TaxConfigurationRepository taxConfigurationRepository,
            TaxBracketRepository taxBracketRepository,
            EmployeePayrollProfileRepository payrollProfileRepository,
            EmployeeRepository employeeRepository,
            AuditService auditService
    ) {
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.taxBracketRepository = taxBracketRepository;
        this.payrollProfileRepository = payrollProfileRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TaxConfigurationResponse createTaxConfiguration(String name, String description, String lineItemType) {

        if (taxConfigurationRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tax configuration already exists: " + name);
        }

        TaxConfiguration configuration = taxConfigurationRepository.save(
                new TaxConfiguration(name, description, lineItemType)
        );

        auditService.log(
                "TAX_CONFIGURATION_CREATED", "TaxConfiguration", configuration.getId(), AuditService.RESULT_SUCCESS
        );

        return TaxConfigurationResponse.from(configuration);
    }

    public List<TaxConfigurationResponse> getActiveTaxConfigurations() {
        return taxConfigurationRepository.findByActiveTrue().stream()
                .map(TaxConfigurationResponse::from)
                .toList();
    }

    @Transactional
    public TaxBracketResponse addTaxBracket(
            UUID taxConfigurationId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal ratePercent,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        if (!taxConfigurationRepository.existsById(taxConfigurationId)) {
            throw new PayrollNotFoundException("Tax configuration not found: " + taxConfigurationId);
        }

        if (maxAmount != null && maxAmount.compareTo(minAmount) <= 0) {
            throw new BusinessRuleException("Maximum amount must be greater than minimum amount");
        }

        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessRuleException("Effective-to date cannot be before effective-from date");
        }

        TaxBracket bracket = taxBracketRepository.save(
                new TaxBracket(taxConfigurationId, minAmount, maxAmount, ratePercent, effectiveFrom, effectiveTo)
        );

        auditService.log("TAX_BRACKET_CREATED", "TaxBracket", bracket.getId(), AuditService.RESULT_SUCCESS);

        return TaxBracketResponse.from(bracket);
    }

    public List<TaxBracketResponse> getTaxBrackets(UUID taxConfigurationId) {
        return taxBracketRepository.findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigurationId).stream()
                .map(TaxBracketResponse::from)
                .toList();
    }

    /**
     * Progressive-bracket tax calculation: a generic algorithm over
     * whatever brackets are configured for this tax and effective on
     * {@code asOfDate} - contains no rate assumptions of its own. A
     * flat-rate-with-ceiling tax (e.g. UIF) is naturally handled by a
     * single configured bracket.
     */
    public BigDecimal calculateTax(UUID taxConfigurationId, BigDecimal taxableAmount, LocalDate asOfDate) {

        List<TaxBracket> effectiveBrackets = taxBracketRepository
                .findByTaxConfigurationIdOrderByMinAmountAsc(taxConfigurationId)
                .stream()
                .filter(bracket -> bracket.isEffectiveOn(asOfDate))
                .toList();

        BigDecimal tax = BigDecimal.ZERO;

        for (TaxBracket bracket : effectiveBrackets) {

            if (taxableAmount.compareTo(bracket.getMinAmount()) <= 0) {
                continue;
            }

            BigDecimal upperBound = bracket.getMaxAmount() == null
                    ? taxableAmount
                    : taxableAmount.min(bracket.getMaxAmount());

            BigDecimal amountInBracket = upperBound.subtract(bracket.getMinAmount());

            if (amountInBracket.signum() <= 0) {
                continue;
            }

            tax = tax.add(
                    amountInBracket
                            .multiply(bracket.getRatePercent())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );
        }

        return tax;
    }

    @Transactional
    public EmployeePayrollProfileResponse upsertPayrollProfile(
            UUID employeeId,
            BigDecimal baseSalary,
            String payFrequency,
            BigDecimal standardHoursPerPeriod,
            BigDecimal overtimeHourlyRate,
            String bankAccountReference,
            String taxNumber
    ) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        }

        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> new EmployeePayrollProfile(
                        employeeId, baseSalary, payFrequency, standardHoursPerPeriod,
                        overtimeHourlyRate, bankAccountReference, taxNumber
                ));

        profile.update(
                baseSalary, payFrequency, standardHoursPerPeriod,
                overtimeHourlyRate, bankAccountReference, taxNumber
        );

        EmployeePayrollProfile saved = payrollProfileRepository.save(profile);

        auditService.log(
                "PAYROLL_PROFILE_SAVED", "EmployeePayrollProfile", saved.getId(), AuditService.RESULT_SUCCESS
        );

        return EmployeePayrollProfileResponse.from(saved);
    }

    public EmployeePayrollProfileResponse getPayrollProfile(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeePayroll(employeeId, requesterUserId, requesterCanManage);

        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new PayrollNotFoundException("Payroll profile not found for employee: " + employeeId));

        return EmployeePayrollProfileResponse.from(profile);
    }

    @Transactional
    public EmployeePayrollProfileResponse deactivateProfile(UUID employeeId) {
        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new PayrollNotFoundException("Payroll profile not found for employee: " + employeeId));

        profile.deactivate();

        return EmployeePayrollProfileResponse.from(profile);
    }

    private void assertCanAccessEmployeePayroll(
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
            throw new AccessDeniedException("You do not have permission to access this employee's payroll information");
        }
    }
}
