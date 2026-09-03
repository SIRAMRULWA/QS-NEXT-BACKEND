package za.co.qsnext.employeemanagement.reporting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.reporting.dto.AttendanceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.EmployeeReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.LeaveReportResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

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
}