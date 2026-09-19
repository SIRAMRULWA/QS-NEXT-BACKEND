package za.co.qsnext.employeemanagement.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.calendar.CalendarEvent;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.SchedulingNotFoundException;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftAssignmentResponse;
import za.co.qsnext.employeemanagement.scheduling.dto.ShiftResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CalendarService calendarService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                shiftRepository, shiftAssignmentRepository, employeeRepository, calendarService);
    }

    private Employee employeeWithId(UUID id, UUID userId, UUID departmentId) {
        Employee employee = new Employee(
                userId, departmentId, "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private Shift shiftWithId(UUID id, String name, LocalTime start, LocalTime end, UUID departmentId) {
        Shift shift = new Shift(name, start, end, departmentId);
        setId(shift, id);
        return shift;
    }

    @Test
    void createShift_savesTheShift() {
        when(shiftRepository.save(any())).thenAnswer(invocation -> {
            Shift shift = invocation.getArgument(0);
            setId(shift, UUID.randomUUID());
            return shift;
        });

        ShiftResponse response = scheduleService.createShift(
                "Morning", LocalTime.of(8, 0), LocalTime.of(16, 0), UUID.randomUUID());

        assertThat(response.name()).isEqualTo("Morning");
    }

    @Test
    void createShift_rejectsEqualStartAndEndTime() {
        LocalTime time = LocalTime.of(8, 0);

        assertThatThrownBy(() -> scheduleService.createShift("Bad", time, time, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getAllShifts_returnsAllShifts() {
        Shift shift = shiftWithId(
                UUID.randomUUID(), "Morning", LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        when(shiftRepository.findAll()).thenReturn(List.of(shift));

        List<ShiftResponse> shifts = scheduleService.getAllShifts();

        assertThat(shifts).hasSize(1);
        assertThat(shifts.getFirst().name()).isEqualTo("Morning");
    }

    @Test
    void assignShift_createsAssignmentAndRecordsCalendarEvent() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 2, 2);

        Employee employee = employeeWithId(employeeId, userId, departmentId);
        Shift shift = shiftWithId(shiftId, "Morning", LocalTime.of(8, 0), LocalTime.of(16, 0), departmentId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shiftAssignmentRepository.existsByEmployeeIdAndWorkDateAndStatus(
                employeeId, workDate, ShiftAssignment.STATUS_SCHEDULED)).thenReturn(false);
        when(shiftAssignmentRepository.save(any())).thenAnswer(invocation -> {
            ShiftAssignment assignment = invocation.getArgument(0);
            setId(assignment, UUID.randomUUID());
            return assignment;
        });

        ShiftAssignmentResponse response =
                scheduleService.assignShift(employeeId, shiftId, workDate, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(ShiftAssignment.STATUS_SCHEDULED);
        verify(calendarService).recordSystemEvent(
                any(), eq(CalendarEvent.TYPE_SHIFT), any(), any(), eq(userId), eq(departmentId));
    }

    @Test
    void assignShift_handlesOvernightShiftEndDate() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 2, 2);

        Employee employee = employeeWithId(employeeId, userId, departmentId);
        Shift shift = shiftWithId(shiftId, "Night", LocalTime.of(22, 0), LocalTime.of(6, 0), departmentId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shiftAssignmentRepository.existsByEmployeeIdAndWorkDateAndStatus(
                employeeId, workDate, ShiftAssignment.STATUS_SCHEDULED)).thenReturn(false);
        when(shiftAssignmentRepository.save(any())).thenAnswer(invocation -> {
            ShiftAssignment assignment = invocation.getArgument(0);
            setId(assignment, UUID.randomUUID());
            return assignment;
        });

        scheduleService.assignShift(employeeId, shiftId, workDate, UUID.randomUUID());

        verify(calendarService).recordSystemEvent(
                any(), eq(CalendarEvent.TYPE_SHIFT),
                eq(workDate.atTime(22, 0).atOffset(java.time.ZoneOffset.UTC)),
                eq(workDate.plusDays(1).atTime(6, 0).atOffset(java.time.ZoneOffset.UTC)),
                eq(userId), eq(departmentId));
    }

    @Test
    void assignShift_rejectsDuplicateActiveAssignment() {
        UUID employeeId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 2, 2);

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), UUID.randomUUID())));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(
                shiftWithId(shiftId, "Morning", LocalTime.of(8, 0), LocalTime.of(16, 0), null)));
        when(shiftAssignmentRepository.existsByEmployeeIdAndWorkDateAndStatus(
                employeeId, workDate, ShiftAssignment.STATUS_SCHEDULED)).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.assignShift(employeeId, shiftId, workDate, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);

        verify(shiftAssignmentRepository, never()).save(any());
    }

    @Test
    void assignShift_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.assignShift(
                employeeId, UUID.randomUUID(), LocalDate.now(), UUID.randomUUID()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void assignShift_throws_whenShiftDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID(), UUID.randomUUID())));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.assignShift(
                employeeId, shiftId, LocalDate.now(), UUID.randomUUID()))
                .isInstanceOf(SchedulingNotFoundException.class);
    }

    @Test
    void cancelAssignment_marksTheAssignmentCancelled() {
        UUID assignmentId = UUID.randomUUID();
        ShiftAssignment assignment = new ShiftAssignment(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), UUID.randomUUID());
        setId(assignment, assignmentId);

        when(shiftAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        scheduleService.cancelAssignment(assignmentId);

        assertThat(assignment.getStatus()).isEqualTo(ShiftAssignment.STATUS_CANCELLED);
    }

    @Test
    void cancelAssignment_throws_whenAssignmentDoesNotExist() {
        UUID assignmentId = UUID.randomUUID();
        when(shiftAssignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.cancelAssignment(assignmentId))
                .isInstanceOf(SchedulingNotFoundException.class);
    }

    @Test
    void getEmployeeSchedule_rejectsAnEndBeforeStart() {
        UUID employeeId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 2, 10);

        assertThatThrownBy(() ->
                scheduleService.getEmployeeSchedule(employeeId, start, start.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getOwnSchedule_resolvesEmployeeByUserId() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);

        Employee employee = employeeWithId(employeeId, userId, UUID.randomUUID());
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                employeeId, start, end)).thenReturn(List.of());

        List<ShiftAssignmentResponse> schedule = scheduleService.getOwnSchedule(userId, start, end);

        assertThat(schedule).isEmpty();
    }

    @Test
    void getOwnSchedule_throws_whenNoEmployeeProfile() {
        UUID userId = UUID.randomUUID();
        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getOwnSchedule(
                userId, LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(EmployeeNotFoundException.class);
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
