package za.co.qsnext.employeemanagement.reporting;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.attendance.Attendance;
import za.co.qsnext.employeemanagement.attendance.AttendanceRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveRequest;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.reporting.dto.AttendanceReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.EmployeeReportResponse;
import za.co.qsnext.employeemanagement.reporting.dto.LeaveReportResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;

    public ReportService(
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
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

    private void verifyEmployeeExists(UUID employeeId) {

        if (!employeeRepository.existsById(employeeId)) {

            throw new EmployeeNotFoundException(
                    "Employee not found: " + employeeId
            );
        }
    }
}