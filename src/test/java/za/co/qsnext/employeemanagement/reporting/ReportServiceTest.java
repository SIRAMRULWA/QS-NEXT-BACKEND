package za.co.qsnext.employeemanagement.reporting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import za.co.qsnext.employeemanagement.compliance.ComplianceRecord;
import za.co.qsnext.employeemanagement.compliance.ComplianceRecordRepository;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.expense.ExpenseClaim;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.learning.CourseEnrollment;
import za.co.qsnext.employeemanagement.learning.CourseEnrollmentRepository;
import za.co.qsnext.employeemanagement.learning.EmployeeSkill;
import za.co.qsnext.employeemanagement.learning.EmployeeSkillRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunEntry;
import za.co.qsnext.employeemanagement.payroll.PayrollRunEntryRepository;
import za.co.qsnext.employeemanagement.performance.PerformanceReview;
import za.co.qsnext.employeemanagement.performance.PerformanceReviewRepository;
import za.co.qsnext.employeemanagement.recruitment.Application;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;
import za.co.qsnext.employeemanagement.recruitment.JobPostingRepository;
import za.co.qsnext.employeemanagement.reporting.dto.ComplianceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.DepartmentReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.ExpenseReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.LearningReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.PayrollReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.PerformanceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.RecruitmentReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.TimesheetReportResponse;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetEntryRepository;
import za.co.qsnext.employeemanagement.timesheet.TimesheetHoursSummary;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;
import za.co.qsnext.employeemanagement.attendance.AttendanceRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;
    @Mock
    private PayrollRunEntryRepository payrollRunEntryRepository;
    @Mock
    private ExpenseClaimRepository expenseClaimRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private PerformanceReviewRepository performanceReviewRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private ComplianceRecordRepository complianceRecordRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                employeeRepository, leaveRequestRepository, attendanceRepository, departmentRepository,
                timesheetRepository, timesheetEntryRepository, payrollRunEntryRepository, expenseClaimRepository,
                jobPostingRepository, applicationRepository, performanceReviewRepository,
                courseEnrollmentRepository, employeeSkillRepository, complianceRecordRepository);
    }

    private Employee employee(UUID departmentId) {
        return new Employee(
                UUID.randomUUID(), departmentId, "EMP-1", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
    }

    @Test
    void getDepartmentReport_throws_whenDepartmentDoesNotExist() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getDepartmentReport(departmentId))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void getDepartmentReport_returnsHeadcountAndEmployeeList() {
        UUID departmentId = UUID.randomUUID();
        Department department = new Department("Engineering", "Builds things");
        setId(department, departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeRepository.findByDepartmentId(departmentId, PageRequest.of(0, 1000)))
                .thenReturn(new PageImpl<>(List.of(employee(departmentId))));

        DepartmentReportResponse response = reportService.getDepartmentReport(departmentId);

        assertThat(response.name()).isEqualTo("Engineering");
        assertThat(response.employeeCount()).isEqualTo(1);
    }

    @Test
    void getTimesheetReport_sumsApprovedHoursFromAGroupedQuery() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        Timesheet approved = new Timesheet(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        UUID timesheetId = UUID.randomUUID();
        setId(approved, timesheetId);
        approved.approve(UUID.randomUUID());

        when(timesheetRepository.findByEmployeeIdAndPeriodStartBetweenOrderByPeriodStartAsc(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(approved));

        TimesheetHoursSummary hoursSummary = mock(TimesheetHoursSummary.class);
        when(hoursSummary.getTimesheetId()).thenReturn(timesheetId);
        when(hoursSummary.getTotalHours()).thenReturn(BigDecimal.valueOf(160));

        when(timesheetEntryRepository.sumHoursGroupedByTimesheetId(List.of(timesheetId)))
                .thenReturn(List.of(hoursSummary));

        TimesheetReportResponse response = reportService.getTimesheetReport(employeeId, 2026);

        assertThat(response.approvedTimesheets()).isEqualTo(1);
        assertThat(response.totalApprovedHours()).isEqualByComparingTo("160");
    }

    @Test
    void getPayrollReport_sumsEarningsDeductionsAndNetPay() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        PayrollRunEntry entry = new PayrollRunEntry(UUID.randomUUID(), employeeId);
        entry.recalculate(List.of(
                new za.co.qsnext.employeemanagement.payroll.PayrollLineItem(
                        UUID.randomUUID(), "EARNING", "BASE", "Base salary", BigDecimal.valueOf(50000))
        ));

        when(payrollRunEntryRepository.findByEmployeeIdAndPayPeriodBetween(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(entry));

        PayrollReportResponse response = reportService.getPayrollReport(employeeId, 2026);

        assertThat(response.payslipCount()).isEqualTo(1);
        assertThat(response.totalEarnings()).isEqualByComparingTo("50000");
    }

    @Test
    void getExpenseReport_countsClaimsByOutcome() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        ExpenseClaim reimbursed = new ExpenseClaim(
                employeeId, UUID.randomUUID(), BigDecimal.valueOf(200), "ZAR", "Taxi",
                LocalDate.of(2026, 3, 1), null);
        reimbursed.submit();
        reimbursed.approve(UUID.randomUUID());
        reimbursed.markReimbursed();

        when(expenseClaimRepository.findByEmployeeIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(reimbursed));

        ExpenseReportResponse response = reportService.getExpenseReport(employeeId, 2026);

        assertThat(response.reimbursedClaims()).isEqualTo(1);
        assertThat(response.totalReimbursedAmount()).isEqualByComparingTo("200");
    }

    @Test
    void getRecruitmentReport_throws_whenJobPostingDoesNotExist() {
        UUID jobPostingId = UUID.randomUUID();
        when(jobPostingRepository.findById(jobPostingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getRecruitmentReport(jobPostingId))
                .isInstanceOf(RecruitmentNotFoundException.class);
    }

    @Test
    void getRecruitmentReport_summarizesApplicationsByStage() {
        UUID jobPostingId = UUID.randomUUID();
        JobPosting jobPosting = new JobPosting(UUID.randomUUID(), "Engineer", "Desc", "Remote", "FULL_TIME");
        setId(jobPosting, jobPostingId);

        when(jobPostingRepository.findById(jobPostingId)).thenReturn(Optional.of(jobPosting));

        Application hired = new Application(UUID.randomUUID(), jobPostingId);
        hired.advanceToScreening();
        hired.advanceToInterviewing();
        hired.advanceToOffer();
        hired.markHired();

        when(applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId))
                .thenReturn(List.of(hired));

        RecruitmentReportResponse response = reportService.getRecruitmentReport(jobPostingId);

        assertThat(response.totalApplications()).isEqualTo(1);
        assertThat(response.hiredApplications()).isEqualTo(1);
    }

    @Test
    void getPerformanceReport_averagesManagerRatings() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        PerformanceReview review = new PerformanceReview(UUID.randomUUID(), employeeId, UUID.randomUUID());
        review.submitSelfAssessment(4, "Good work");
        review.submitManagerAssessment(5, "Great work");

        when(performanceReviewRepository.findByEmployeeId(employeeId)).thenReturn(List.of(review));

        PerformanceReportResponse response = reportService.getPerformanceReport(employeeId);

        assertThat(response.completedReviews()).isEqualTo(1);
        assertThat(response.averageManagerRating()).isEqualTo(5.0);
    }

    @Test
    void getLearningReport_combinesEnrollmentsAndSkills() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        CourseEnrollment enrollment = new CourseEnrollment(employeeId, UUID.randomUUID());
        enrollment.updateProgress(100);

        EmployeeSkill skill = new EmployeeSkill(employeeId, UUID.randomUUID(), "ADVANCED");

        when(courseEnrollmentRepository.findByEmployeeId(employeeId)).thenReturn(List.of(enrollment));
        when(employeeSkillRepository.findByEmployeeId(employeeId)).thenReturn(List.of(skill));

        LearningReportResponse response = reportService.getLearningReport(employeeId);

        assertThat(response.completedEnrollments()).isEqualTo(1);
        assertThat(response.skills()).hasSize(1);
    }

    @Test
    void getComplianceReport_countsRecordsByStatus() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        ComplianceRecord record = new ComplianceRecord(employeeId, UUID.randomUUID());
        record.complete(UUID.randomUUID(), "Verified", UUID.randomUUID(), null);

        when(complianceRecordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(record));

        ComplianceReportResponse response = reportService.getComplianceReport(employeeId);

        assertThat(response.completedRecords()).isEqualTo(1);
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
