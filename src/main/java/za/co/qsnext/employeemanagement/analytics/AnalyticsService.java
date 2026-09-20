package za.co.qsnext.employeemanagement.analytics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import za.co.qsnext.employeemanagement.analytics.dto.StatusCount;
import za.co.qsnext.employeemanagement.analytics.dto.TurnoverAnalyticsResponse;
import za.co.qsnext.employeemanagement.attendance.AttendanceRepository;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.PayrollNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.learning.CourseEnrollmentRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollLineItemRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRun;
import za.co.qsnext.employeemanagement.payroll.PayrollRunEntryRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunSummary;
import za.co.qsnext.employeemanagement.performance.PerformanceReview;
import za.co.qsnext.employeemanagement.performance.PerformanceReviewRepository;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.expense.ExpenseTrendSummary;
import za.co.qsnext.employeemanagement.recognition.RecognitionRepository;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Org-wide, cross-employee analytics dashboards. Unlike {@code
 * ReportService} (which fetches and aggregates in Java for a single
 * employee), every method here does its aggregation as real SQL -
 * COUNT/SUM/GROUP BY via Spring Data interface projections - and never
 * fetches an entire table into memory.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final String TERMINATED_STATUS = "TERMINATED";

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollLineItemRepository payrollLineItemRepository;
    private final PayrollRunEntryRepository payrollRunEntryRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final ApplicationRepository applicationRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final RecognitionRepository recognitionRepository;

    public AnalyticsService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            PayrollLineItemRepository payrollLineItemRepository,
            PayrollRunEntryRepository payrollRunEntryRepository,
            PayrollRunRepository payrollRunRepository,
            ApplicationRepository applicationRepository,
            ExpenseClaimRepository expenseClaimRepository,
            PerformanceReviewRepository performanceReviewRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            RecognitionRepository recognitionRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollLineItemRepository = payrollLineItemRepository;
        this.payrollRunEntryRepository = payrollRunEntryRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.applicationRepository = applicationRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.performanceReviewRepository = performanceReviewRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.recognitionRepository = recognitionRepository;
    }

    public HeadcountAnalyticsResponse getHeadcountAnalytics() {
        long total = employeeRepository.count();
        List<StatusCount> byStatus = employeeRepository.countGroupedByEmploymentStatus().stream()
                .map(row -> new StatusCount(row.getStatus(), row.getEmployeeCount()))
                .toList();
        return new HeadcountAnalyticsResponse(total, byStatus);
    }

    public DepartmentDistributionResponse getDepartmentDistribution() {
        var counts = employeeRepository.countGroupedByDepartment();

        Map<UUID, String> departmentNames = departmentRepository
                .findAllById(counts.stream().map(row -> row.getDepartmentId()).toList()).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        List<DepartmentDistributionResponse.DepartmentCount> departments = counts.stream()
                .map(row -> new DepartmentDistributionResponse.DepartmentCount(
                        row.getDepartmentId(),
                        departmentNames.getOrDefault(row.getDepartmentId(), "Unknown"),
                        row.getEmployeeCount()))
                .toList();

        return new DepartmentDistributionResponse(departments);
    }

    /**
     * "Terminations" is approximated as employees whose {@code
     * employmentStatus} is currently TERMINATED and whose {@code
     * updatedAt} falls in the window - {@code Employee} has no
     * dedicated termination-date field, and {@code updatedAt} is
     * touched by any profile edit, not only a status change. Treat
     * this figure as indicative, not exact.
     */
    public TurnoverAnalyticsResponse getTurnoverAnalytics(LocalDate from, LocalDate to) {
        long hires = employeeRepository.countByHireDateBetween(from, to);

        OffsetDateTime windowStart = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime windowEnd = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        long terminations = employeeRepository.countByEmploymentStatusAndUpdatedAtBetween(
                TERMINATED_STATUS, windowStart, windowEnd);

        return new TurnoverAnalyticsResponse(from, to, hires, terminations,
                "Terminations are approximated from employees currently marked TERMINATED whose "
                        + "record was last updated in this window; this is not a precise termination-date count.");
    }

    public AttendanceTrendResponse getAttendanceTrend(LocalDate from, LocalDate to) {
        List<StatusCount> byStatus = attendanceRepository.countGroupedByStatus(from, to).stream()
                .map(row -> new StatusCount(row.getStatus(), row.getRecordCount()))
                .toList();
        return new AttendanceTrendResponse(from, to, byStatus);
    }

    public LeaveTrendResponse getLeaveTrend(LocalDate from, LocalDate to) {
        List<StatusCount> byStatus = leaveRequestRepository.countGroupedByStatus(from, to).stream()
                .map(row -> new StatusCount(row.getStatus(), row.getRequestCount()))
                .toList();
        return new LeaveTrendResponse(from, to, byStatus);
    }

    public OvertimeAnalyticsResponse getOvertimeAnalytics(LocalDate from, LocalDate to) {
        BigDecimal total = payrollLineItemRepository.sumOvertimeAmountForPeriod(from, to);
        return new OvertimeAnalyticsResponse(from, to, total);
    }

    public RecruitmentFunnelResponse getRecruitmentFunnel() {
        List<StatusCount> byStage = applicationRepository.countGroupedByStatus().stream()
                .map(row -> new StatusCount(row.getStatus(), row.getApplicationCount()))
                .toList();
        return new RecruitmentFunnelResponse(byStage);
    }

    public PayrollSummaryAnalyticsResponse getPayrollSummary(UUID payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new PayrollNotFoundException("Payroll run not found: " + payrollRunId));

        PayrollRunSummary summary = payrollRunEntryRepository.summarizeByPayrollRunId(run.getId());

        return new PayrollSummaryAnalyticsResponse(
                run.getId(),
                summary.getTotalEarnings(),
                summary.getTotalDeductions(),
                summary.getTotalEmployerContributions(),
                summary.getTotalNetPay(),
                summary.getEmployeeCount());
    }

    public ExpenseTrendAnalyticsResponse getExpenseTrend(LocalDate from, LocalDate to) {
        ExpenseTrendSummary summary = expenseClaimRepository.summarizeForPeriod(from, to);
        return new ExpenseTrendAnalyticsResponse(from, to, summary.getTotalAmount(), summary.getClaimCount());
    }

    public PerformanceSummaryAnalyticsResponse getPerformanceSummary() {
        long draft = performanceReviewRepository.countByStatus(PerformanceReview.STATUS_DRAFT);
        long inProgress = performanceReviewRepository.countByStatus(PerformanceReview.STATUS_IN_PROGRESS);
        long completed = performanceReviewRepository.countByStatus(PerformanceReview.STATUS_COMPLETED);
        Double averageRating = performanceReviewRepository.averageCompletedManagerRating();

        return new PerformanceSummaryAnalyticsResponse(draft, inProgress, completed, averageRating);
    }

    public LearningActivityAnalyticsResponse getLearningActivity() {
        List<StatusCount> byStatus = courseEnrollmentRepository.countGroupedByStatus().stream()
                .map(row -> new StatusCount(row.getStatus(), row.getEnrollmentCount()))
                .toList();
        return new LearningActivityAnalyticsResponse(byStatus);
    }

    public EngagementAnalyticsResponse getEngagementAnalytics() {
        long total = recognitionRepository.count();
        long totalPoints = recognitionRepository.sumAllPoints();
        long distinctGivers = recognitionRepository.countDistinctGivers();
        long distinctRecipients = recognitionRepository.countDistinctRecipients();

        return new EngagementAnalyticsResponse(total, totalPoints, distinctGivers, distinctRecipients);
    }
}
