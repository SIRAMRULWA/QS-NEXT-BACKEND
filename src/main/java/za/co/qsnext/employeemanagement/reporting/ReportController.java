package za.co.qsnext.employeemanagement.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

import java.util.UUID;

@Tag(name = "Reports", description = "Per-employee and per-scope operational reports.")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get employee report")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<EmployeeReportResponse> getEmployeeReport(
            @PathVariable UUID employeeId
    ) {

        return ResponseEntity.ok(
                reportService.getEmployeeReport(
                        employeeId
                )
        );
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get leave report")
    @GetMapping("/leave/{employeeId}")
    public ResponseEntity<LeaveReportResponse> getLeaveReport(
            @PathVariable UUID employeeId,
            @RequestParam int year
    ) {

        return ResponseEntity.ok(
                reportService.getLeaveReport(
                        employeeId,
                        year
                )
        );
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get attendance report")
    @GetMapping("/attendance/{employeeId}")
    public ResponseEntity<AttendanceReportResponse> getAttendanceReport(
            @PathVariable UUID employeeId,
            @RequestParam int year
    ) {

        return ResponseEntity.ok(
                reportService.getAttendanceReport(
                        employeeId,
                        year
                )
        );
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get department report")
    @GetMapping("/departments/{departmentId}")
    public ResponseEntity<DepartmentReportResponse> getDepartmentReport(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(reportService.getDepartmentReport(departmentId));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get timesheet report")
    @GetMapping("/timesheets/{employeeId}")
    public ResponseEntity<TimesheetReportResponse> getTimesheetReport(
            @PathVariable UUID employeeId,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(reportService.getTimesheetReport(employeeId, year));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get payroll report")
    @GetMapping("/payroll/{employeeId}")
    public ResponseEntity<PayrollReportResponse> getPayrollReport(
            @PathVariable UUID employeeId,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(reportService.getPayrollReport(employeeId, year));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get expense report")
    @GetMapping("/expenses/{employeeId}")
    public ResponseEntity<ExpenseReportResponse> getExpenseReport(
            @PathVariable UUID employeeId,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(reportService.getExpenseReport(employeeId, year));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get recruitment report")
    @GetMapping("/recruitment/{jobPostingId}")
    public ResponseEntity<RecruitmentReportResponse> getRecruitmentReport(@PathVariable UUID jobPostingId) {
        return ResponseEntity.ok(reportService.getRecruitmentReport(jobPostingId));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get performance report")
    @GetMapping("/performance/{employeeId}")
    public ResponseEntity<PerformanceReportResponse> getPerformanceReport(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(reportService.getPerformanceReport(employeeId));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get learning report")
    @GetMapping("/learning/{employeeId}")
    public ResponseEntity<LearningReportResponse> getLearningReport(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(reportService.getLearningReport(employeeId));
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    @Operation(summary = "Get compliance report")
    @GetMapping("/compliance/{employeeId}")
    public ResponseEntity<ComplianceReportResponse> getComplianceReport(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(reportService.getComplianceReport(employeeId));
    }
}