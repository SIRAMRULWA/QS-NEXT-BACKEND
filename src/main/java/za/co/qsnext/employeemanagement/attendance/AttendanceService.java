package za.co.qsnext.employeemanagement.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EmployeeService employeeService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.employeeService = employeeService;
    }

    public Attendance getById(UUID attendanceId) {

        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Attendance record not found: " + attendanceId
                        )
                );
    }

    public Attendance getByEmployeeAndDate(
            UUID employeeId,
            LocalDate date
    ) {

        employeeService.getById(employeeId);

        return attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        date
                )
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Attendance record not found"
                        )
                );
    }

    public Page<Attendance> getByEmployee(
            UUID employeeId,
            Pageable pageable
    ) {

        employeeService.getById(employeeId);

        return attendanceRepository.findByEmployeeId(
                employeeId,
                pageable
        );
    }

    public Page<Attendance> getByDate(
            LocalDate date,
            Pageable pageable
    ) {

        return attendanceRepository.findByAttendanceDate(
                date,
                pageable
        );
    }

    @Transactional
    public Attendance create(
            UUID employeeId,
            LocalDate attendanceDate
    ) {

        employeeService.getById(employeeId);

        if (attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        attendanceDate
                )
                .isPresent()) {

            throw new BusinessRuleException(
                    "Attendance already exists for this employee and date"
            );
        }

        return attendanceRepository.save(
                new Attendance(
                        employeeId,
                        attendanceDate
                )
        );
    }

    @Transactional
    public Attendance clockIn(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        if (attendance.getClockIn() != null) {
            throw new BusinessRuleException(
                    "Employee has already clocked in"
            );
        }

        attendance.clockIn();

        return attendance;
    }

    @Transactional
    public Attendance clockOut(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        if (attendance.getClockIn() == null) {
            throw new BusinessRuleException(
                    "Employee must clock in before clocking out"
            );
        }

        if (attendance.getClockOut() != null) {
            throw new BusinessRuleException(
                    "Employee has already clocked out"
            );
        }

        attendance.clockOut();

        return attendance;
    }

    @Transactional
    public Attendance markAbsent(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        attendance.markAbsent();

        return attendance;
    }

    @Transactional
    public Attendance markLate(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        attendance.markLate();

        return attendance;
    }

    @Transactional
    public Attendance markHalfDay(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        attendance.markHalfDay();

        return attendance;
    }

    @Transactional
    public Attendance markOnLeave(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        attendance.markOnLeave();

        return attendance;
    }

    @Transactional
    public Attendance markRemote(UUID attendanceId) {

        Attendance attendance = getById(attendanceId);

        attendance.markRemote();

        return attendance;
    }

    @Transactional
    public Attendance updateNotes(
            UUID attendanceId,
            String notes
    ) {

        Attendance attendance = getById(attendanceId);

        attendance.updateNotes(notes);

        return attendance;
    }
}