package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveService;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;
import za.co.qsnext.employeemanagement.timesheet.TimesheetService;
import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SelfServiceService {

    private final EmployeeRepository employeeRepository;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;
    private final TimesheetService timesheetService;

    public SelfServiceService(
            EmployeeRepository employeeRepository,
            LeaveService leaveService,
            AttendanceService attendanceService,
            TimesheetService timesheetService
    ) {
        this.employeeRepository = employeeRepository;
        this.leaveService = leaveService;
        this.attendanceService = attendanceService;
        this.timesheetService = timesheetService;
    }

    /*
     * ============================================================
     * PROFILE
     * ============================================================
     */

    public SelfServiceProfileResponse getOwnProfile(
            UUID userId
    ) {

        Employee employee =
                employeeRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new EmployeeNotFoundException(
                                        "Employee profile not found"
                                )
                        );

        return SelfServiceProfileResponse.from(employee);
    }

    /*
     * ============================================================
     * LEAVE
     * ============================================================
     */

    public Page<LeaveResponse> getOwnLeave(
            UUID userId,
            Pageable pageable
    ) {

        return leaveService.getOwnLeave(
                userId,
                pageable
        );
    }

    /*
     * ============================================================
     * ATTENDANCE
     * ============================================================
     */

    public Page<AttendanceResponse> getOwnAttendance(
            UUID userId,
            Pageable pageable
    ) {

        return attendanceService
                .getOwnAttendance(
                        userId,
                        pageable
                )
                .map(AttendanceResponse::from);
    }

    @Transactional
    public AttendanceResponse clockIn(
            UUID userId
    ) {

        return AttendanceResponse.from(
                attendanceService.clockInForUser(
                        userId
                )
        );
    }

    @Transactional
    public AttendanceResponse clockOut(
            UUID userId
    ) {

        return AttendanceResponse.from(
                attendanceService.clockOutForUser(
                        userId
                )
        );
    }

    /*
     * ============================================================
     * TIMESHEETS
     * ============================================================
     */

    public Page<TimesheetResponse> getOwnTimesheets(
            UUID userId,
            Pageable pageable
    ) {

        return timesheetService
                .getOwnTimesheets(
                        userId,
                        pageable
                )
                .map(TimesheetResponse::from);
    }

    @Transactional
    public TimesheetResponse createTimesheet(
            UUID userId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {

        return TimesheetResponse.from(
                timesheetService.createForUser(
                        userId,
                        periodStart,
                        periodEnd
                )
        );
    }

    @Transactional
    public void addTimesheetEntry(
            UUID userId,
            UUID timesheetId,
            LocalDate workDate,
            BigDecimal hoursWorked,
            String description
    ) {

        timesheetService.addEntryForUser(
                userId,
                timesheetId,
                workDate,
                hoursWorked,
                description
        );
    }

    @Transactional
    public TimesheetResponse submitTimesheet(
            UUID userId,
            UUID timesheetId
    ) {

        return TimesheetResponse.from(
                timesheetService.submitForUser(
                        userId,
                        timesheetId
                )
        );
    }
}