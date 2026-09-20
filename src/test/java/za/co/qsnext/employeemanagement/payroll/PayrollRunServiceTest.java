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
import za.co.qsnext.employeemanagement.exception.PayrollNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.payroll.dto.PayPeriodResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunEntryResponse;
import za.co.qsnext.employeemanagement.payroll.dto.PayrollRunResponse;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetEntryRepository;
import za.co.qsnext.employeemanagement.timesheet.TimesheetHoursSummary;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTest {

    @Mock
    private PayPeriodRepository payPeriodRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayrollRunEntryRepository payrollRunEntryRepository;
    @Mock
    private PayrollLineItemRepository payrollLineItemRepository;
    @Mock
    private EmployeePayrollProfileRepository payrollProfileRepository;
    @Mock
    private TaxConfigurationRepository taxConfigurationRepository;
    @Mock
    private PayrollConfigService payrollConfigService;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private AuditService auditService;

    private PayrollRunService payrollRunService;

    @BeforeEach
    void setUp() {
        payrollRunService = new PayrollRunService(
                payPeriodRepository, payrollRunRepository, payrollRunEntryRepository, payrollLineItemRepository,
                payrollProfileRepository, taxConfigurationRepository, payrollConfigService, timesheetRepository,
                timesheetEntryRepository, employeeRepository, notificationPublisher, auditService);
    }

    private PayPeriod periodWithId(UUID id) {
        PayPeriod period = new PayPeriod(
                "2026-01 Monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 5));
        setId(period, id);
        return period;
    }

    private PayrollRun runWithId(UUID id, UUID payPeriodId) {
        PayrollRun run = new PayrollRun(payPeriodId, UUID.randomUUID());
        setId(run, id);
        return run;
    }

    private PayrollRunEntry entryWithId(UUID id, UUID runId, UUID employeeId) {
        PayrollRunEntry entry = new PayrollRunEntry(runId, employeeId);
        setId(entry, id);
        return entry;
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    @Test
    void createPayPeriod_rejectsAnEndDateBeforeStartDate() {
        when(payPeriodRepository.existsByName("Bad Period")).thenReturn(false);
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> payrollRunService.createPayPeriod(
                "Bad Period", start, start.minusDays(1), start))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createPayPeriod_rejectsAPayDateBeforeTheEndDate() {
        when(payPeriodRepository.existsByName("Bad Period")).thenReturn(false);
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);

        assertThatThrownBy(() -> payrollRunService.createPayPeriod(
                "Bad Period", start, end, end.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createPayPeriod_rejectsADuplicateName() {
        when(payPeriodRepository.existsByName("2026-01")).thenReturn(true);

        assertThatThrownBy(() -> payrollRunService.createPayPeriod(
                "2026-01", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 5)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createRun_rejectsWhenARunAlreadyExistsForThePeriod() {
        UUID periodId = UUID.randomUUID();

        when(payPeriodRepository.findById(periodId)).thenReturn(Optional.of(periodWithId(periodId)));
        when(payrollRunRepository.findByPayPeriodId(periodId))
                .thenReturn(Optional.of(runWithId(UUID.randomUUID(), periodId)));

        assertThatThrownBy(() -> payrollRunService.createRun(periodId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createRun_generatesAnEntryWithBasicSalaryAndTaxForEachActiveProfile() {
        UUID periodId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID taxConfigId = UUID.randomUUID();

        PayPeriod period = periodWithId(periodId);
        EmployeePayrollProfile profile = new EmployeePayrollProfile(
                employeeId, BigDecimal.valueOf(50000), EmployeePayrollProfile.FREQUENCY_MONTHLY,
                null, null, null, null);

        TaxConfiguration taxConfig = new TaxConfiguration("PAYE", "Desc", TaxConfiguration.LINE_ITEM_TYPE_DEDUCTION);
        setId(taxConfig, taxConfigId);

        when(payPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayPeriodId(periodId)).thenReturn(Optional.empty());
        when(payrollRunRepository.save(any())).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            setId(run, UUID.randomUUID());
            return run;
        });
        when(taxConfigurationRepository.findByActiveTrue()).thenReturn(List.of(taxConfig));
        when(payrollProfileRepository.findByActiveTrue()).thenReturn(List.of(profile));
        when(payrollRunEntryRepository.save(any())).thenAnswer(invocation -> {
            PayrollRunEntry entry = invocation.getArgument(0);
            setId(entry, UUID.randomUUID());
            return entry;
        });

        List<PayrollLineItem> savedLineItems = new ArrayList<>();
        when(payrollLineItemRepository.save(any())).thenAnswer(invocation -> {
            PayrollLineItem lineItem = invocation.getArgument(0);
            savedLineItems.add(lineItem);
            return lineItem;
        });
        when(payrollLineItemRepository.findByPayrollRunEntryId(any())).thenAnswer(invocation -> savedLineItems);
        when(payrollConfigService.calculateTax(eq(taxConfigId), any(), any())).thenReturn(BigDecimal.valueOf(5000));

        PayrollRunResponse response = payrollRunService.createRun(periodId, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(PayrollRun.STATUS_DRAFT);
        assertThat(savedLineItems).hasSize(2);
        assertThat(savedLineItems.stream().anyMatch(item -> "BASIC_SALARY".equals(item.getCode()))).isTrue();
        assertThat(savedLineItems.stream().anyMatch(item -> "PAYE".equals(item.getCode()))).isTrue();
    }

    @Test
    void createRun_includesOvertime_whenApprovedTimesheetExceedsStandardHours() {
        UUID periodId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID timesheetId = UUID.randomUUID();

        PayPeriod period = periodWithId(periodId);
        EmployeePayrollProfile profile = new EmployeePayrollProfile(
                employeeId, BigDecimal.valueOf(50000), EmployeePayrollProfile.FREQUENCY_MONTHLY,
                BigDecimal.valueOf(160), BigDecimal.valueOf(250), null, null);

        Timesheet timesheet = new Timesheet(employeeId, period.getStartDate(), period.getEndDate());
        setId(timesheet, timesheetId);

        when(payPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayPeriodId(periodId)).thenReturn(Optional.empty());
        when(payrollRunRepository.save(any())).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            setId(run, UUID.randomUUID());
            return run;
        });
        when(taxConfigurationRepository.findByActiveTrue()).thenReturn(List.of());
        when(payrollProfileRepository.findByActiveTrue()).thenReturn(List.of(profile));
        when(payrollRunEntryRepository.save(any())).thenAnswer(invocation -> {
            PayrollRunEntry e = invocation.getArgument(0);
            setId(e, UUID.randomUUID());
            return e;
        });
        when(timesheetRepository.findByEmployeeIdInAndPeriodStartAndPeriodEndAndStatus(
                List.of(employeeId), period.getStartDate(), period.getEndDate(), "APPROVED"))
                .thenReturn(List.of(timesheet));

        TimesheetHoursSummary hoursSummary = mock(TimesheetHoursSummary.class);
        when(hoursSummary.getTimesheetId()).thenReturn(timesheetId);
        when(hoursSummary.getTotalHours()).thenReturn(BigDecimal.valueOf(170));

        when(timesheetEntryRepository.sumHoursGroupedByTimesheetId(List.of(timesheetId)))
                .thenReturn(List.of(hoursSummary));

        List<PayrollLineItem> savedLineItems = new ArrayList<>();
        when(payrollLineItemRepository.save(any())).thenAnswer(invocation -> {
            PayrollLineItem lineItem = invocation.getArgument(0);
            savedLineItems.add(lineItem);
            return lineItem;
        });
        when(payrollLineItemRepository.findByPayrollRunEntryId(any())).thenAnswer(invocation -> savedLineItems);

        payrollRunService.createRun(periodId, UUID.randomUUID());

        // 170 total hours - 160 standard = 10 overtime hours * 250 = 2500
        PayrollLineItem overtime = savedLineItems.stream()
                .filter(item -> "OVERTIME".equals(item.getCode()))
                .findFirst()
                .orElseThrow();

        assertThat(overtime.getAmount()).isEqualByComparingTo("2500");
    }

    @Test
    void addLineItem_throws_whenRunIsNotDraft() {
        UUID entryId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        PayrollRunEntry entry = entryWithId(entryId, runId, UUID.randomUUID());
        PayrollRun run = runWithId(runId, UUID.randomUUID());
        run.approve(UUID.randomUUID());

        when(payrollRunEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollRunService.addLineItem(
                entryId, PayrollLineItem.TYPE_EARNING, "BONUS", "Desc", BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addLineItem_recalculatesEntryTotals() {
        UUID entryId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        PayrollRunEntry entry = entryWithId(entryId, runId, UUID.randomUUID());
        PayrollRun run = runWithId(runId, UUID.randomUUID());

        PayrollLineItem existingLine = new PayrollLineItem(
                entryId, PayrollLineItem.TYPE_EARNING, "BASIC_SALARY", "Desc", BigDecimal.valueOf(50000));
        PayrollLineItem newLine = new PayrollLineItem(
                entryId, PayrollLineItem.TYPE_EARNING, "BONUS", "Desc", BigDecimal.valueOf(5000));

        when(payrollRunEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(payrollLineItemRepository.save(any())).thenReturn(newLine);
        when(payrollLineItemRepository.findByPayrollRunEntryId(entryId))
                .thenReturn(List.of(existingLine, newLine));

        PayrollRunEntryResponse response = payrollRunService.addLineItem(
                entryId, PayrollLineItem.TYPE_EARNING, "BONUS", "Desc", BigDecimal.valueOf(5000));

        assertThat(response.totalEarnings()).isEqualByComparingTo("55000");
        assertThat(response.netPay()).isEqualByComparingTo("55000");
    }

    @Test
    void approveRun_throws_whenNotDraft() {
        UUID runId = UUID.randomUUID();
        PayrollRun run = runWithId(runId, UUID.randomUUID());
        run.approve(UUID.randomUUID());

        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollRunService.approveRun(runId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markRunPaid_throws_whenNotApproved() {
        UUID runId = UUID.randomUUID();
        PayrollRun run = runWithId(runId, UUID.randomUUID());

        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollRunService.markRunPaid(runId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markRunPaid_notifiesEachEmployee() {
        UUID runId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PayrollRun run = runWithId(runId, UUID.randomUUID());
        run.approve(UUID.randomUUID());
        PayrollRunEntry entry = entryWithId(UUID.randomUUID(), runId, employeeId);

        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(payrollRunEntryRepository.findByPayrollRunId(runId)).thenReturn(List.of(entry));
        when(employeeRepository.findAllById(List.of(employeeId)))
                .thenReturn(List.of(employeeWithId(employeeId, userId)));

        PayrollRunResponse response = payrollRunService.markRunPaid(runId);

        assertThat(response.status()).isEqualTo(PayrollRun.STATUS_PAID);
        verify(notificationPublisher).publish(eq(userId), eq(NotificationType.PAYSLIP_AVAILABLE), any(), any());
    }

    @Test
    void getPayslip_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID entryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        PayrollRunEntry entry = entryWithId(entryId, UUID.randomUUID(), employeeId);

        when(payrollRunEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> payrollRunService.getPayslip(entryId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMyPayslips_resolvesEmployeeByUserId() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findByUserId(userId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(payrollRunEntryRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)).thenReturn(List.of());

        assertThat(payrollRunService.getMyPayslips(userId)).isEmpty();
    }

    @Test
    void getRun_throws_whenRunDoesNotExist() {
        UUID runId = UUID.randomUUID();
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollRunService.getRun(runId))
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
