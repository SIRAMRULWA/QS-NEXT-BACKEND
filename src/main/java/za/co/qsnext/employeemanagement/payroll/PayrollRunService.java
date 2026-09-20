package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.payroll.dto.PayPeriodResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollLineItemResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunEntryResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayslipResponse;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetEntryRepository;
import za.co.qsnext.employeemanagement.timesheet.TimesheetHoursSummary;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs payroll for a {@link PayPeriod}: generates one {@link PayrollRunEntry}
 * per active {@link EmployeePayrollProfile} with EARNING lines for base
 * salary and (where configured) overtime sourced from APPROVED
 * timesheets, and DEDUCTION/EMPLOYER_CONTRIBUTION lines from every
 * active {@link TaxConfiguration} via {@link PayrollConfigService#calculateTax}.
 */
@Service
@Transactional(readOnly = true)
public class PayrollRunService {

    private static final String TIMESHEET_STATUS_APPROVED = "APPROVED";

    private final PayPeriodRepository payPeriodRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunEntryRepository payrollRunEntryRepository;
    private final PayrollLineItemRepository payrollLineItemRepository;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final TaxConfigurationRepository taxConfigurationRepository;
    private final PayrollConfigService payrollConfigService;
    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditService auditService;

    public PayrollRunService(
            PayPeriodRepository payPeriodRepository,
            PayrollRunRepository payrollRunRepository,
            PayrollRunEntryRepository payrollRunEntryRepository,
            PayrollLineItemRepository payrollLineItemRepository,
            EmployeePayrollProfileRepository payrollProfileRepository,
            TaxConfigurationRepository taxConfigurationRepository,
            PayrollConfigService payrollConfigService,
            TimesheetRepository timesheetRepository,
            TimesheetEntryRepository timesheetEntryRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher,
            AuditService auditService
    ) {
        this.payPeriodRepository = payPeriodRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunEntryRepository = payrollRunEntryRepository;
        this.payrollLineItemRepository = payrollLineItemRepository;
        this.payrollProfileRepository = payrollProfileRepository;
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.payrollConfigService = payrollConfigService;
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public PayPeriodResponse createPayPeriod(String name, LocalDate startDate, LocalDate endDate, LocalDate payDate) {

        if (payPeriodRepository.existsByName(name)) {
            throw new DuplicateResourceException("Pay period already exists: " + name);
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        if (payDate.isBefore(endDate)) {
            throw new BusinessRuleException("Pay date cannot be before the period's end date");
        }

        return PayPeriodResponse.from(payPeriodRepository.save(new PayPeriod(name, startDate, endDate, payDate)));
    }

    public Page<PayPeriodResponse> getAllPayPeriods(Pageable pageable) {
        return payPeriodRepository.findAll(pageable).map(PayPeriodResponse::from);
    }

    @Transactional
    public PayrollRunResponse createRun(UUID payPeriodId, UUID runByUserId) {

        PayPeriod period = payPeriodRepository.findById(payPeriodId)
                .orElseThrow(() -> new PayrollNotFoundException("Pay period not found: " + payPeriodId));

        if (payrollRunRepository.findByPayPeriodId(payPeriodId).isPresent()) {
            throw new BusinessRuleException("A payroll run already exists for this pay period");
        }

        PayrollRun run = payrollRunRepository.save(new PayrollRun(payPeriodId, runByUserId));

        List<TaxConfiguration> activeTaxConfigurations = taxConfigurationRepository.findByActiveTrue();
        List<EmployeePayrollProfile> activeProfiles = payrollProfileRepository.findByActiveTrue();

        Map<UUID, BigDecimal> overtimeAmountsByEmployeeId = calculateOvertimeAmounts(activeProfiles, period);

        for (EmployeePayrollProfile profile : activeProfiles) {
            generateEntry(
                    run, period, profile, activeTaxConfigurations,
                    overtimeAmountsByEmployeeId.get(profile.getEmployeeId())
            );
        }

        auditService.log("PAYROLL_RUN_CREATED", "PayrollRun", run.getId(), AuditService.RESULT_SUCCESS);

        return PayrollRunResponse.from(run);
    }

    private void generateEntry(
            PayrollRun run,
            PayPeriod period,
            EmployeePayrollProfile profile,
            List<TaxConfiguration> activeTaxConfigurations,
            BigDecimal overtimeAmount
    ) {
        PayrollRunEntry entry = payrollRunEntryRepository.save(
                new PayrollRunEntry(run.getId(), profile.getEmployeeId())
        );

        payrollLineItemRepository.save(new PayrollLineItem(
                entry.getId(), PayrollLineItem.TYPE_EARNING, "BASIC_SALARY", "Base salary", profile.getBaseSalary()
        ));

        if (overtimeAmount != null && overtimeAmount.signum() > 0) {
            payrollLineItemRepository.save(new PayrollLineItem(
                    entry.getId(), PayrollLineItem.TYPE_EARNING, "OVERTIME",
                    "Overtime pay for " + period.getName(), overtimeAmount
            ));
        }

        BigDecimal grossEarnings = profile.getBaseSalary()
                .add(overtimeAmount == null ? BigDecimal.ZERO : overtimeAmount);

        for (TaxConfiguration taxConfiguration : activeTaxConfigurations) {

            BigDecimal taxAmount = payrollConfigService.calculateTax(
                    taxConfiguration.getId(), grossEarnings, period.getPayDate()
            );

            if (taxAmount.signum() > 0) {
                payrollLineItemRepository.save(new PayrollLineItem(
                        entry.getId(), taxConfiguration.getLineItemType(), taxConfiguration.getName(),
                        taxConfiguration.getName() + " for " + period.getName(), taxAmount
                ));
            }
        }

        entry.recalculate(payrollLineItemRepository.findByPayrollRunEntryId(entry.getId()));
    }

    /**
     * Batch form of the per-profile overtime calculation: one query for every
     * approved timesheet across all overtime-eligible employees for this
     * period, plus one aggregate query for their total hours, instead of the
     * previous one-timesheet-lookup-plus-one-entries-fetch pair per employee.
     */
    private Map<UUID, BigDecimal> calculateOvertimeAmounts(
            List<EmployeePayrollProfile> profiles, PayPeriod period
    ) {
        List<EmployeePayrollProfile> overtimeEligibleProfiles = profiles.stream()
                .filter(profile -> profile.getStandardHoursPerPeriod() != null
                        && profile.getOvertimeHourlyRate() != null)
                .toList();

        if (overtimeEligibleProfiles.isEmpty()) {
            return Map.of();
        }

        List<UUID> employeeIds = overtimeEligibleProfiles.stream()
                .map(EmployeePayrollProfile::getEmployeeId)
                .toList();

        List<Timesheet> approvedTimesheets = timesheetRepository
                .findByEmployeeIdInAndPeriodStartAndPeriodEndAndStatus(
                        employeeIds, period.getStartDate(), period.getEndDate(), TIMESHEET_STATUS_APPROVED
                );

        if (approvedTimesheets.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UUID> timesheetIdByEmployeeId = approvedTimesheets.stream()
                .collect(Collectors.toMap(Timesheet::getEmployeeId, Timesheet::getId));

        Map<UUID, BigDecimal> totalHoursByTimesheetId = timesheetEntryRepository
                .sumHoursGroupedByTimesheetId(approvedTimesheets.stream().map(Timesheet::getId).toList())
                .stream()
                .collect(Collectors.toMap(TimesheetHoursSummary::getTimesheetId, TimesheetHoursSummary::getTotalHours));

        Map<UUID, BigDecimal> overtimeAmountsByEmployeeId = new HashMap<>();

        for (EmployeePayrollProfile profile : overtimeEligibleProfiles) {

            UUID timesheetId = timesheetIdByEmployeeId.get(profile.getEmployeeId());

            if (timesheetId == null) {
                continue;
            }

            BigDecimal totalHours = totalHoursByTimesheetId.getOrDefault(timesheetId, BigDecimal.ZERO);
            BigDecimal overtimeHours = totalHours.subtract(profile.getStandardHoursPerPeriod());

            if (overtimeHours.signum() > 0) {
                overtimeAmountsByEmployeeId.put(
                        profile.getEmployeeId(), overtimeHours.multiply(profile.getOvertimeHourlyRate())
                );
            }
        }

        return overtimeAmountsByEmployeeId;
    }

    public PayrollRunResponse getRun(UUID runId) {
        return PayrollRunResponse.from(findRunOrThrow(runId));
    }

    public List<PayrollRunEntryResponse> getRunEntries(UUID runId) {
        return payrollRunEntryRepository.findByPayrollRunId(runId).stream()
                .map(PayrollRunEntryResponse::from)
                .toList();
    }

    @Transactional
    public PayrollRunEntryResponse addLineItem(
            UUID entryId,
            String type,
            String code,
            String description,
            BigDecimal amount
    ) {
        PayrollRunEntry entry = findEntryOrThrow(entryId);
        PayrollRun run = findRunOrThrow(entry.getPayrollRunId());

        if (!run.isDraft()) {
            throw new BusinessRuleException("Can only add line items to a draft payroll run");
        }

        payrollLineItemRepository.save(new PayrollLineItem(entry.getId(), type, code, description, amount));

        entry.recalculate(payrollLineItemRepository.findByPayrollRunEntryId(entry.getId()));

        auditService.log(
                "PAYROLL_LINE_ITEM_ADDED", "PayrollRunEntry", entry.getId(), AuditService.RESULT_SUCCESS
        );

        return PayrollRunEntryResponse.from(entry);
    }

    @Transactional
    public PayrollRunResponse approveRun(UUID runId, UUID approvedByUserId) {

        PayrollRun run = findRunOrThrow(runId);

        if (!run.isDraft()) {
            throw new BusinessRuleException("Only a draft payroll run can be approved");
        }

        run.approve(approvedByUserId);

        auditService.log("PAYROLL_RUN_APPROVED", "PayrollRun", runId, AuditService.RESULT_SUCCESS);

        return PayrollRunResponse.from(run);
    }

    @Transactional
    public PayrollRunResponse markRunPaid(UUID runId) {

        PayrollRun run = findRunOrThrow(runId);

        if (!run.isApproved()) {
            throw new BusinessRuleException("Only an approved payroll run can be marked as paid");
        }

        run.markPaid();

        List<PayrollRunEntry> entries = payrollRunEntryRepository.findByPayrollRunId(runId);

        Map<UUID, Employee> employeesById = employeeRepository
                .findAllById(entries.stream().map(PayrollRunEntry::getEmployeeId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        for (PayrollRunEntry entry : entries) {

            Employee employee = employeesById.get(entry.getEmployeeId());

            if (employee != null) {
                notificationPublisher.publish(
                        employee.getUserId(),
                        NotificationType.PAYSLIP_AVAILABLE,
                        "Payslip available",
                        "Your payslip is ready - net pay: " + entry.getNetPay() + "."
                );
            }
        }

        auditService.log("PAYROLL_RUN_PAID", "PayrollRun", runId, AuditService.RESULT_SUCCESS);

        return PayrollRunResponse.from(run);
    }

    public PayslipResponse getPayslip(UUID entryId, UUID requesterUserId, boolean requesterCanManage) {

        PayrollRunEntry entry = findEntryOrThrow(entryId);
        assertCanAccessEmployeePayroll(entry.getEmployeeId(), requesterUserId, requesterCanManage);

        PayrollRun run = findRunOrThrow(entry.getPayrollRunId());
        PayPeriod period = payPeriodRepository.findById(run.getPayPeriodId())
                .orElseThrow(() -> new PayrollNotFoundException("Pay period not found: " + run.getPayPeriodId()));

        List<PayrollLineItemResponse> lineItems = payrollLineItemRepository
                .findByPayrollRunEntryId(entryId).stream()
                .map(PayrollLineItemResponse::from)
                .toList();

        return new PayslipResponse(PayrollRunEntryResponse.from(entry), PayPeriodResponse.from(period), lineItems);
    }

    public List<PayrollRunEntryResponse> getPayslipsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeePayroll(employeeId, requesterUserId, requesterCanManage);

        return payrollRunEntryRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(PayrollRunEntryResponse::from)
                .toList();
    }

    public List<PayrollRunEntryResponse> getMyPayslips(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return payrollRunEntryRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(PayrollRunEntryResponse::from)
                .toList();
    }

    private PayrollRun findRunOrThrow(UUID runId) {
        return payrollRunRepository.findById(runId)
                .orElseThrow(() -> new PayrollNotFoundException("Payroll run not found: " + runId));
    }

    private PayrollRunEntry findEntryOrThrow(UUID entryId) {
        return payrollRunEntryRepository.findById(entryId)
                .orElseThrow(() -> new PayrollNotFoundException("Payroll run entry not found: " + entryId));
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
