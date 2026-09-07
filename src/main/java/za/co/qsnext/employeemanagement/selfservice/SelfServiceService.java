package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.leave.LeaveService;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SelfServiceService {

    private final EmployeeRepository employeeRepository;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;

    public SelfServiceService(
            EmployeeRepository employeeRepository, LeaveService leaveService, AttendanceService attendanceService
    ) {
        this.employeeRepository = employeeRepository;
        this.leaveService = leaveService;
        this.attendanceService = attendanceService;
    }

    public SelfServiceProfileResponse getOwnProfile(
            UUID userId
    ) {
        Employee employee = employeeRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee profile not found"
                        )
                );

        return SelfServiceProfileResponse.from(employee);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> getOwnLeave(
            UUID userId,
            Pageable pageable
    ) {
        return leaveService.getOwnLeave(
                userId,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getOwnAttendance(
            UUID userId,
            Pageable pageable
    ) {
        return attendanceService
                .getOwnAttendance(userId, pageable)
                .map(AttendanceResponse::from);
    }

    @Transactional
    public AttendanceResponse clockIn(
            UUID userId
    ) {
        return AttendanceResponse.from(
                attendanceService.clockInForUser(userId)
        );
    }

    @Transactional
    public AttendanceResponse clockOut(
            UUID userId
    ) {
        return AttendanceResponse.from(
                attendanceService.clockOutForUser(userId)
        );
    }
}