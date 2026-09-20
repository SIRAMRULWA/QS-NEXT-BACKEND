package za.co.qsnext.employeemanagement.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.analytics.dto.AttendanceTrendResponse;
import za.co.qsnext.employeemanagement.analytics.dto.DepartmentDistributionResponse;
import za.co.qsnext.employeemanagement.analytics.dto.EngagementAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.ExpenseTrendAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.HeadcountAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.LeaveTrendResponse;
import za.co.qsnext.employeemanagement.analytics.dto.LearningActivityAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.OvertimeAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.PayrollSummaryAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.PerformanceSummaryAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.RecruitmentFunnelResponse;
import za.co.qsnext.employeemanagement.analytics.dto.TurnoverAnalyticsResponse;
import za.co.qsnext.employeemanagement.attendance.AttendanceRepository;
import za.co.qsnext.employeemanagement.attendance.AttendanceStatusCount;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.DepartmentHeadcount;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.employee.EmploymentStatusCount;
import za.co.qsnext.employeemanagement.exception.PayrollNotFoundException;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.expense.ExpenseTrendSummary;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.leave.LeaveStatusCount;
import za.co.qsnext.employeemanagement.learning.CourseEnrollmentRepository;
import za.co.qsnext.employeemanagement.learning.EnrollmentStatusCount;
import za.co.qsnext.employeemanagement.payroll.PayrollLineItemRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRun;
import za.co.qsnext.employeemanagement.payroll.PayrollRunEntryRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunSummary;
import za.co.qsnext.employeemanagement.performance.PerformanceReview;
import za.co.qsnext.employeemanagement.performance.PerformanceReviewRepository;
import za.co.qsnext.employeemanagement.recognition.RecognitionRepository;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.recruitment.ApplicationStatusCount;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private PayrollLineItemRepository payrollLineItemRepository;
    @Mock
    private PayrollRunEntryRepository payrollRunEntryRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ExpenseClaimRepository expenseClaimRepository;
    @Mock
    private PerformanceReviewRepository performanceReviewRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private RecognitionRepository recognitionRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                employeeRepository, departmentRepository, attendanceRepository, leaveRequestRepository,
                payrollLineItemRepository, payrollRunEntryRepository, payrollRunRepository,
                applicationRepository, expenseClaimRepository, performanceReviewRepository,
                courseEnrollmentRepository, recognitionRepository);
    }

    @Test
    void getHeadcountAnalytics_combinesTotalAndPerStatusCounts() {
        when(employeeRepository.count()).thenReturn(10L);

        EmploymentStatusCount active = mock(EmploymentStatusCount.class);
        when(active.getStatus()).thenReturn("ACTIVE");
        when(active.getEmployeeCount()).thenReturn(9L);

        EmploymentStatusCount terminated = mock(EmploymentStatusCount.class);
        when(terminated.getStatus()).thenReturn("TERMINATED");
        when(terminated.getEmployeeCount()).thenReturn(1L);

        when(employeeRepository.countGroupedByEmploymentStatus()).thenReturn(List.of(active, terminated));

        HeadcountAnalyticsResponse response = analyticsService.getHeadcountAnalytics();

        assertThat(response.totalEmployees()).isEqualTo(10);
        assertThat(response.byEmploymentStatus()).hasSize(2);
    }

    @Test
    void getDepartmentDistribution_resolvesDepartmentNamesInBatch() {
        UUID departmentId = UUID.randomUUID();

        DepartmentHeadcount headcount = mock(DepartmentHeadcount.class);
        when(headcount.getDepartmentId()).thenReturn(departmentId);
        when(headcount.getEmployeeCount()).thenReturn(5L);

        when(employeeRepository.countGroupedByDepartment()).thenReturn(List.of(headcount));

        Department department = new Department("Engineering", "Builds things");
        setId(department, departmentId);
        when(departmentRepository.findAllById(List.of(departmentId))).thenReturn(List.of(department));

        DepartmentDistributionResponse response = analyticsService.getDepartmentDistribution();

        assertThat(response.departments()).hasSize(1);
        assertThat(response.departments().get(0).departmentName()).isEqualTo("Engineering");
        assertThat(response.departments().get(0).employeeCount()).isEqualTo(5);
    }

    @Test
    void getTurnoverAnalytics_reportsHiresAndApproximatedTerminations() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(employeeRepository.countByHireDateBetween(from, to)).thenReturn(3L);
        when(employeeRepository.countByEmploymentStatusAndUpdatedAtBetween(
                org.mockito.ArgumentMatchers.eq("TERMINATED"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);

        TurnoverAnalyticsResponse response = analyticsService.getTurnoverAnalytics(from, to);

        assertThat(response.hires()).isEqualTo(3);
        assertThat(response.terminations()).isEqualTo(1);
        assertThat(response.terminationMethodology()).isNotBlank();
    }

    @Test
    void getAttendanceTrend_mapsStatusBreakdown() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        AttendanceStatusCount present = mock(AttendanceStatusCount.class);
        when(present.getStatus()).thenReturn("PRESENT");
        when(present.getRecordCount()).thenReturn(20L);

        when(attendanceRepository.countGroupedByStatus(from, to)).thenReturn(List.of(present));

        AttendanceTrendResponse response = analyticsService.getAttendanceTrend(from, to);

        assertThat(response.byStatus()).hasSize(1);
        assertThat(response.byStatus().get(0).status()).isEqualTo("PRESENT");
        assertThat(response.byStatus().get(0).count()).isEqualTo(20);
    }

    @Test
    void getLeaveTrend_mapsStatusBreakdown() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        LeaveStatusCount approved = mock(LeaveStatusCount.class);
        when(approved.getStatus()).thenReturn("APPROVED");
        when(approved.getRequestCount()).thenReturn(4L);

        when(leaveRequestRepository.countGroupedByStatus(from, to)).thenReturn(List.of(approved));

        LeaveTrendResponse response = analyticsService.getLeaveTrend(from, to);

        assertThat(response.byStatus()).hasSize(1);
        assertThat(response.byStatus().get(0).count()).isEqualTo(4);
    }

    @Test
    void getOvertimeAnalytics_returnsSummedAmount() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(payrollLineItemRepository.sumOvertimeAmountForPeriod(from, to)).thenReturn(BigDecimal.valueOf(500));

        OvertimeAnalyticsResponse response = analyticsService.getOvertimeAnalytics(from, to);

        assertThat(response.totalOvertimeAmount()).isEqualByComparingTo("500");
    }

    @Test
    void getRecruitmentFunnel_mapsStageBreakdown() {
        ApplicationStatusCount screening = mock(ApplicationStatusCount.class);
        when(screening.getStatus()).thenReturn("SCREENING");
        when(screening.getApplicationCount()).thenReturn(7L);

        when(applicationRepository.countGroupedByStatus()).thenReturn(List.of(screening));

        RecruitmentFunnelResponse response = analyticsService.getRecruitmentFunnel();

        assertThat(response.byStage()).hasSize(1);
        assertThat(response.byStage().get(0).status()).isEqualTo("SCREENING");
    }

    @Test
    void getPayrollSummary_throws_whenRunDoesNotExist() {
        UUID runId = UUID.randomUUID();
        when(payrollRunRepository.findById(runId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> analyticsService.getPayrollSummary(runId))
                .isInstanceOf(PayrollNotFoundException.class);
    }

    @Test
    void getPayrollSummary_returnsAggregatedTotals() {
        UUID payPeriodId = UUID.randomUUID();
        PayrollRun run = new PayrollRun(payPeriodId, UUID.randomUUID());
        UUID runId = UUID.randomUUID();
        setId(run, runId);

        when(payrollRunRepository.findById(runId)).thenReturn(java.util.Optional.of(run));

        PayrollRunSummary summary = mock(PayrollRunSummary.class);
        when(summary.getTotalEarnings()).thenReturn(BigDecimal.valueOf(100000));
        when(summary.getTotalDeductions()).thenReturn(BigDecimal.valueOf(10000));
        when(summary.getTotalEmployerContributions()).thenReturn(BigDecimal.ZERO);
        when(summary.getTotalNetPay()).thenReturn(BigDecimal.valueOf(90000));
        when(summary.getEmployeeCount()).thenReturn(2L);

        when(payrollRunEntryRepository.summarizeByPayrollRunId(runId)).thenReturn(summary);

        PayrollSummaryAnalyticsResponse response = analyticsService.getPayrollSummary(runId);

        assertThat(response.totalNetPay()).isEqualByComparingTo("90000");
        assertThat(response.employeeCount()).isEqualTo(2);
    }

    @Test
    void getExpenseTrend_returnsTotalsForThePeriod() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        ExpenseTrendSummary summary = mock(ExpenseTrendSummary.class);
        when(summary.getTotalAmount()).thenReturn(BigDecimal.valueOf(1500));
        when(summary.getClaimCount()).thenReturn(3L);

        when(expenseClaimRepository.summarizeForPeriod(from, to)).thenReturn(summary);

        ExpenseTrendAnalyticsResponse response = analyticsService.getExpenseTrend(from, to);

        assertThat(response.totalAmount()).isEqualByComparingTo("1500");
        assertThat(response.claimCount()).isEqualTo(3);
    }

    @Test
    void getPerformanceSummary_combinesStatusCountsAndAverageRating() {
        when(performanceReviewRepository.countByStatus(PerformanceReview.STATUS_DRAFT)).thenReturn(1L);
        when(performanceReviewRepository.countByStatus(PerformanceReview.STATUS_IN_PROGRESS)).thenReturn(2L);
        when(performanceReviewRepository.countByStatus(PerformanceReview.STATUS_COMPLETED)).thenReturn(5L);
        when(performanceReviewRepository.averageCompletedManagerRating()).thenReturn(4.2);

        PerformanceSummaryAnalyticsResponse response = analyticsService.getPerformanceSummary();

        assertThat(response.draftReviews()).isEqualTo(1);
        assertThat(response.completedReviews()).isEqualTo(5);
        assertThat(response.averageCompletedManagerRating()).isEqualTo(4.2);
    }

    @Test
    void getLearningActivity_mapsStatusBreakdown() {
        EnrollmentStatusCount completed = mock(EnrollmentStatusCount.class);
        when(completed.getStatus()).thenReturn("COMPLETED");
        when(completed.getEnrollmentCount()).thenReturn(6L);

        when(courseEnrollmentRepository.countGroupedByStatus()).thenReturn(List.of(completed));

        LearningActivityAnalyticsResponse response = analyticsService.getLearningActivity();

        assertThat(response.byStatus()).hasSize(1);
        assertThat(response.byStatus().get(0).count()).isEqualTo(6);
    }

    @Test
    void getEngagementAnalytics_combinesCountsAndPoints() {
        when(recognitionRepository.count()).thenReturn(15L);
        when(recognitionRepository.sumAllPoints()).thenReturn(300L);
        when(recognitionRepository.countDistinctGivers()).thenReturn(8L);
        when(recognitionRepository.countDistinctRecipients()).thenReturn(10L);

        EngagementAnalyticsResponse response = analyticsService.getEngagementAnalytics();

        assertThat(response.totalRecognitions()).isEqualTo(15);
        assertThat(response.totalPoints()).isEqualTo(300);
        assertThat(response.distinctGivers()).isEqualTo(8);
        assertThat(response.distinctRecipients()).isEqualTo(10);
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
