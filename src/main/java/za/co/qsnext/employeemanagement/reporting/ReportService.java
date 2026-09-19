package za.co.qsnext.employeemanagement.reporting;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.attendance.Attendance;
import za.co.qsnext.employeemanagement.attendance.AttendanceRepository;
import za.co.qsnext.employeemanagement.compliance.ComplianceRecordRepository;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.leave.LeaveRequest;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.learning.CourseEnrollmentRepository;
import za.co.qsnext.employeemanagement.learning.EmployeeSkillRepository;
import za.co.qsnext.employeemanagement.payroll.PayrollRunEntryRepository;
import za.co.qsnext.employeemanagement.performance.PerformanceReviewRepository;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;
import za.co.qsnext.employeemanagement.recruitment.JobPostingRepository;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.reporting.dto.AttendanceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.ComplianceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.DepartmentReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.EmployeeReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.ExpenseReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.LeaveReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.LearningReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.PayrollReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.PerformanceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.RecruitmentReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.TimesheetReportResponse;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetEntryRepository;
import za.co.qsnext.employeemanagement.timesheet.TimesheetHoursSummary;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final DepartmentRepository departmentRepository;
    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final PayrollRunEntryRepository payrollRunEntryRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final ComplianceRecordRepository complianceRecordRepository;

    public ReportService(
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            AttendanceRepository attendanceRepository,
            DepartmentRepository departmentRepository,
            TimesheetRepository timesheetRepository,
            TimesheetEntryRepository timesheetEntryRepository,
            PayrollRunEntryRepository payrollRunEntryRepository,
            ExpenseClaimRepository expenseClaimRepository,
            JobPostingRepository jobPostingRepository,
            ApplicationRepository applicationRepository,
            PerformanceReviewRepository performanceReviewRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            EmployeeSkillRepository employeeSkillRepository,
            ComplianceRecordRepository complianceRecordRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.departmentRepository = departmentRepository;
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.payrollRunEntryRepository = payrollRunEntryRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.performanceReviewRepository = performanceReviewRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.complianceRecordRepository = complianceRecordRepository;
    }

    public EmployeeReportResponse getEmployeeReport(UUID employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee not found: " + employeeId
                        )
                );

        return EmployeeReportResponse.from(employee);
    }

    public LeaveReportResponse getLeaveReport(
            UUID employeeId,
            int year
    ) {

        verifyEmployeeExists(employeeId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<LeaveRequest> leaveRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                employeeId,
                                endDate,
                                startDate,
                                PageRequest.of(0, 1000)
                        )
                        .getContent();

        return LeaveReportResponse.from(
                employeeId,
                year,
                leaveRequests
        );
    }

    public AttendanceReportResponse getAttendanceReport(
            UUID employeeId,
            int year
    ) {

        verifyEmployeeExists(employeeId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Attendance> attendanceRecords =
                attendanceRepository
                        .findByEmployeeIdAndAttendanceDateBetween(
                                employeeId,
                                startDate,
                                endDate,
                                PageRequest.of(0, 1000)
                        )
                        .getContent();

        return AttendanceReportResponse.from(
                employeeId,
                year,
                attendanceRecords
        );
    }

    public DepartmentReportResponse getDepartmentReport(UUID departmentId) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + departmentId));

        List<Employee> employees = employeeRepository
                .findByDepartmentId(departmentId, PageRequest.of(0, 1000))
                .getContent();

        return DepartmentReportResponse.from(department, employees);
    }

    public TimesheetReportResponse getTimesheetReport(UUID employeeId, int year) {

        verifyEmployeeExists(employeeId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Timesheet> timesheets = timesheetRepository
                .findByEmployeeIdAndPeriodStartBetweenOrderByPeriodStartAsc(employeeId, startDate, endDate);

        Map<UUID, BigDecimal> hoursByTimesheetId = timesheets.isEmpty()
                ? Map.of()
                : timesheetEntryRepository
                        .sumHoursGroupedByTimesheetId(timesheets.stream().map(Timesheet::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                TimesheetHoursSummary::getTimesheetId, TimesheetHoursSummary::getTotalHours));

        return TimesheetReportResponse.from(employeeId, year, timesheets, hoursByTimesheetId);
    }

    public PayrollReportResponse getPayrollReport(UUID employeeId, int year) {

        verifyEmployeeExists(employeeId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        var entries = payrollRunEntryRepository
                .findByEmployeeIdAndPayPeriodBetween(employeeId, startDate, endDate);

        return PayrollReportResponse.from(employeeId, year, entries);
    }

    public ExpenseReportResponse getExpenseReport(UUID employeeId, int year) {

        verifyEmployeeExists(employeeId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        var claims = expenseClaimRepository
                .findByEmployeeIdAndExpenseDateBetweenOrderByExpenseDateDesc(employeeId, startDate, endDate);

        return ExpenseReportResponse.from(employeeId, year, claims);
    }

    public RecruitmentReportResponse getRecruitmentReport(UUID jobPostingId) {

        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Job posting not found: " + jobPostingId));

        var applications = applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId);

        return RecruitmentReportResponse.from(jobPosting, applications);
    }

    public PerformanceReportResponse getPerformanceReport(UUID employeeId) {

        verifyEmployeeExists(employeeId);

        var reviews = performanceReviewRepository.findByEmployeeId(employeeId);

        return PerformanceReportResponse.from(employeeId, reviews);
    }

    public LearningReportResponse getLearningReport(UUID employeeId) {

        verifyEmployeeExists(employeeId);

        var enrollments = courseEnrollmentRepository.findByEmployeeId(employeeId);
        var skills = employeeSkillRepository.findByEmployeeId(employeeId);

        return LearningReportResponse.from(employeeId, enrollments, skills);
    }

    public ComplianceReportResponse getComplianceReport(UUID employeeId) {

        verifyEmployeeExists(employeeId);

        var records = complianceRecordRepository.findByEmployeeId(employeeId);

        return ComplianceReportResponse.from(employeeId, records);
    }

    private void verifyEmployeeExists(UUID employeeId) {

        if (!employeeRepository.existsById(employeeId)) {

            throw new EmployeeNotFoundException(
                    "Employee not found: " + employeeId
            );
        }
    }
}