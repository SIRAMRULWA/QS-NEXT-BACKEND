package za.co.qsnext.employeemanagement.scheduling;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.calendar.CalendarEvent;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.SchedulingNotFoundException;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftAssignmentResponse;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CalendarService calendarService;

    public ScheduleService(
            ShiftRepository shiftRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            EmployeeRepository employeeRepository,
            CalendarService calendarService
    ) {
        this.shiftRepository = shiftRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.employeeRepository = employeeRepository;
        this.calendarService = calendarService;
    }

    @Transactional
    public ShiftResponse createShift(
            String name,
            LocalTime startTime,
            LocalTime endTime,
            UUID departmentId
    ) {
        if (startTime.equals(endTime)) {
            throw new BusinessRuleException("Shift start and end time cannot be the same");
        }

        return ShiftResponse.from(
                shiftRepository.save(new Shift(name, startTime, endTime, departmentId))
        );
    }

    public List<ShiftResponse> getAllShifts() {
        return shiftRepository.findAll().stream().map(ShiftResponse::from).toList();
    }

    @Transactional
    public ShiftAssignmentResponse assignShift(
            UUID employeeId,
            UUID shiftId,
            LocalDate workDate,
            UUID assignedByUserId
    ) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new SchedulingNotFoundException("Shift not found: " + shiftId));

        if (shiftAssignmentRepository.existsByEmployeeIdAndWorkDateAndStatus(
                employeeId, workDate, ShiftAssignment.STATUS_SCHEDULED)) {
            throw new BusinessRuleException(
                    "Employee already has an active shift assignment for " + workDate
            );
        }

        ShiftAssignment assignment = shiftAssignmentRepository.save(
                new ShiftAssignment(employeeId, shiftId, workDate, assignedByUserId)
        );

        OffsetDateTime startAt = workDate.atTime(shift.getStartTime()).atOffset(ZoneOffset.UTC);
        LocalDate endDate = shift.isOvernight() ? workDate.plusDays(1) : workDate;
        OffsetDateTime endAt = endDate.atTime(shift.getEndTime()).atOffset(ZoneOffset.UTC);

        calendarService.recordSystemEvent(
                shift.getName() + " shift",
                CalendarEvent.TYPE_SHIFT,
                startAt,
                endAt,
                employee.getUserId(),
                employee.getDepartmentId()
        );

        return ShiftAssignmentResponse.from(assignment);
    }

    @Transactional
    public void cancelAssignment(UUID assignmentId) {

        ShiftAssignment assignment = shiftAssignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new SchedulingNotFoundException("Shift assignment not found: " + assignmentId)
                );

        assignment.cancel();
    }

    public List<ShiftAssignmentResponse> getEmployeeSchedule(
            UUID employeeId,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        if (rangeEnd.isBefore(rangeStart)) {
            throw new BusinessRuleException("Range end cannot be before range start");
        }

        return shiftAssignmentRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, rangeStart, rangeEnd)
                .stream()
                .map(ShiftAssignmentResponse::from)
                .toList();
    }

    public List<ShiftAssignmentResponse> getOwnSchedule(
            UUID userId,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return getEmployeeSchedule(employee.getId(), rangeStart, rangeEnd);
    }
}
