package za.co.qsnext.employeemanagement.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeRepository employeeRepository;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                attendanceRepository, employeeService, employeeRepository);
    }

    private Attendance attendanceWithId(UUID employeeId, LocalDate date) {
        Attendance attendance = new Attendance(employeeId, date);
        setId(attendance, UUID.randomUUID());
        return attendance;
    }

    private Employee employeeWithId(UUID employeeId, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-001", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, employeeId);
        return employee;
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

    @Test
    void getById_returnsTheRecord_whenItExists() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.getById(attendance.getId());

        assertThat(result).isSameAs(attendance);
    }

    @Test
    void getById_throwsBusinessRuleException_whenTheRecordIsUnknown() {
        UUID attendanceId = UUID.randomUUID();

        when(attendanceRepository.findById(attendanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getById(attendanceId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getByEmployeeAndDate_returnsTheRecord_whenItExists() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);
        Attendance attendance = attendanceWithId(employeeId, date);

        when(employeeService.getById(employeeId)).thenReturn(employeeWithId(employeeId, UUID.randomUUID()));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.getByEmployeeAndDate(employeeId, date);

        assertThat(result).isSameAs(attendance);
    }

    @Test
    void getByEmployeeAndDate_throwsBusinessRuleException_whenNoRecordExistsForThatDate() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(employeeService.getById(employeeId)).thenReturn(employeeWithId(employeeId, UUID.randomUUID()));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getByEmployeeAndDate(employeeId, date))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getByEmployeeAndDate_propagatesEmployeeNotFound_whenTheEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(employeeService.getById(employeeId))
                .thenThrow(new EmployeeNotFoundException("Employee not found: " + employeeId));

        assertThatThrownBy(() -> attendanceService.getByEmployeeAndDate(employeeId, date))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(attendanceRepository, never()).findByEmployeeIdAndAttendanceDate(any(), any());
    }

    @Test
    void getByEmployee_validatesTheEmployeeThenReturnsTheirRecords() {
        UUID employeeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Attendance> page = new PageImpl<>(List.of(attendanceWithId(employeeId, LocalDate.of(2026, 1, 5))));

        when(employeeService.getById(employeeId)).thenReturn(employeeWithId(employeeId, UUID.randomUUID()));
        when(attendanceRepository.findByEmployeeId(employeeId, pageable)).thenReturn(page);

        Page<Attendance> result = attendanceService.getByEmployee(employeeId, pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getByDate_returnsAllRecordsForThatDate() {
        LocalDate date = LocalDate.of(2026, 1, 5);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Attendance> page = new PageImpl<>(List.of(attendanceWithId(UUID.randomUUID(), date)));

        when(attendanceRepository.findByAttendanceDate(date, pageable)).thenReturn(page);

        Page<Attendance> result = attendanceService.getByDate(date, pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void create_savesANewRecord_whenNoneExistsYetForThatEmployeeAndDate() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(employeeService.getById(employeeId)).thenReturn(employeeWithId(employeeId, UUID.randomUUID()));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Attendance result = attendanceService.create(employeeId, date);

        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        assertThat(result.getAttendanceDate()).isEqualTo(date);
        assertThat(result.getStatus()).isEqualTo("PRESENT");
    }

    @Test
    void create_rejectsADuplicateRecord_forTheSameEmployeeAndDate() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(employeeService.getById(employeeId)).thenReturn(employeeWithId(employeeId, UUID.randomUUID()));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.of(attendanceWithId(employeeId, date)));

        assertThatThrownBy(() -> attendanceService.create(employeeId, date))
                .isInstanceOf(BusinessRuleException.class);

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockIn_recordsTheClockInTime_whenNotAlreadyClockedIn() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.clockIn(attendance.getId());

        assertThat(result.getClockIn()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PRESENT");
    }

    @Test
    void clockIn_rejectsADoubleClockIn() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));
        attendance.clockIn();

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.clockIn(attendance.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void clockOut_recordsTheClockOutTime_whenClockedInAndNotYetClockedOut() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));
        attendance.clockIn();

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.clockOut(attendance.getId());

        assertThat(result.getClockOut()).isNotNull();
    }

    @Test
    void clockOut_rejectsClockingOut_whenNeverClockedIn() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.clockOut(attendance.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void clockOut_rejectsADoubleClockOut() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));
        attendance.clockIn();
        attendance.clockOut();

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.clockOut(attendance.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markAbsent_setsTheStatusToAbsent() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.markAbsent(attendance.getId());

        assertThat(result.getStatus()).isEqualTo("ABSENT");
    }

    @Test
    void markLate_setsTheStatusToLate() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.markLate(attendance.getId());

        assertThat(result.getStatus()).isEqualTo("LATE");
    }

    @Test
    void markHalfDay_setsTheStatusToHalfDay() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.markHalfDay(attendance.getId());

        assertThat(result.getStatus()).isEqualTo("HALF_DAY");
    }

    @Test
    void markOnLeave_setsTheStatusToOnLeave() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.markOnLeave(attendance.getId());

        assertThat(result.getStatus()).isEqualTo("ON_LEAVE");
    }

    @Test
    void markRemote_setsTheStatusToRemote() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.markRemote(attendance.getId());

        assertThat(result.getStatus()).isEqualTo("REMOTE");
    }

    @Test
    void updateNotes_setsTheNotes() {
        Attendance attendance = attendanceWithId(UUID.randomUUID(), LocalDate.of(2026, 1, 5));

        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.updateNotes(attendance.getId(), "Working from the client site");

        assertThat(result.getNotes()).isEqualTo("Working from the client site");
    }

    @Test
    void getOwnAttendance_returnsTheEmployeesRecords_whenLinkedToAUser() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Employee employee = employeeWithId(employeeId, userId);
        Page<Attendance> page = new PageImpl<>(List.of(attendanceWithId(employeeId, LocalDate.of(2026, 1, 5))));

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeId(employeeId, pageable)).thenReturn(page);

        Page<Attendance> result = attendanceService.getOwnAttendance(userId, pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getOwnAttendance_throwsBusinessRuleException_whenNoEmployeeIsLinkedToTheUser() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getOwnAttendance(userId, pageable))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getOwnAttendanceForDate_returnsTheRecord_whenItExists() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);
        Employee employee = employeeWithId(employeeId, userId);
        Attendance attendance = attendanceWithId(employeeId, date);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.of(attendance));

        Optional<Attendance> result = attendanceService.getOwnAttendanceForDate(userId, date);

        assertThat(result).contains(attendance);
    }

    @Test
    void getOwnAttendanceForDate_returnsEmpty_whenNotYetClockedInToday() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);
        Employee employee = employeeWithId(employeeId, userId);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date))
                .thenReturn(Optional.empty());

        Optional<Attendance> result = attendanceService.getOwnAttendanceForDate(userId, date);

        assertThat(result).isEmpty();
    }

    @Test
    void getOwnAttendanceForDate_throwsBusinessRuleException_whenNoEmployeeIsLinkedToTheUser() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 5);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getOwnAttendanceForDate(userId, date))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void clockInForUser_createsTodaysRecordAndClocksIn_whenNoneExistsYet() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, userId);
        LocalDate today = LocalDate.now();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance created = invocation.getArgument(0);
            setId(created, UUID.randomUUID());
            when(attendanceRepository.findById(created.getId())).thenReturn(Optional.of(created));
            return created;
        });

        Attendance result = attendanceService.clockInForUser(userId);

        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        assertThat(result.getClockIn()).isNotNull();
    }

    @Test
    void clockInForUser_clocksInTheExistingRecord_whenOneAlreadyExistsForToday() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, userId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceWithId(employeeId, today);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.clockInForUser(userId);

        assertThat(result.getClockIn()).isNotNull();
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockInForUser_rejectsADoubleClockIn() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, userId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceWithId(employeeId, today);
        attendance.clockIn();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.clockInForUser(userId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void clockInForUser_throwsBusinessRuleException_whenNoEmployeeIsLinkedToTheUser() {
        UUID userId = UUID.randomUUID();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockInForUser(userId))
                .isInstanceOf(BusinessRuleException.class);

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockOutForUser_clocksOutTodaysRecord_whenClockedIn() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, userId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceWithId(employeeId, today);
        attendance.clockIn();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.clockOutForUser(userId);

        assertThat(result.getClockOut()).isNotNull();
    }

    @Test
    void clockOutForUser_throwsBusinessRuleException_whenNoRecordExistsForToday() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, userId);
        LocalDate today = LocalDate.now();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOutForUser(userId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void clockOutForUser_throwsBusinessRuleException_whenNoEmployeeIsLinkedToTheUser() {
        UUID userId = UUID.randomUUID();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOutForUser(userId))
                .isInstanceOf(BusinessRuleException.class);
    }
}
