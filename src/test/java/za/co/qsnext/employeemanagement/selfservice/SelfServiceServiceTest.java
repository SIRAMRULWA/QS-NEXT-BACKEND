package za.co.qsnext.employeemanagement.selfservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.attendance.Attendance;
import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveService;
import za.co.qsnext.employeemanagement.leave.dto.LeaveResponse;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetService;
import za.co.qsnext.employeemanagement.timesheet.dto.TimesheetResponse;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfServiceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveService leaveService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private TimesheetService timesheetService;

    private SelfServiceService selfServiceService;

    @BeforeEach
    void setUp() {
        selfServiceService = new SelfServiceService(
                employeeRepository, leaveService, attendanceService, timesheetService);
    }

    private Employee employeeWithId(UUID employeeId, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-001", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, employeeId);
        return employee;
    }

    private Attendance attendanceWithId(UUID employeeId) {
        Attendance attendance = new Attendance(employeeId, LocalDate.of(2026, 1, 5));
        setId(attendance, UUID.randomUUID());
        return attendance;
    }

    private Timesheet timesheetWithId(UUID employeeId) {
        Timesheet timesheet = new Timesheet(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));
        setId(timesheet, UUID.randomUUID());
        return timesheet;
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
    void getOwnProfile_returnsTheProfile_whenTheEmployeeExists() {
        UUID userId = UUID.randomUUID();
        Employee employee = employeeWithId(UUID.randomUUID(), userId);

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

        SelfServiceProfileResponse response = selfServiceService.getOwnProfile(userId);

        assertThat(response.employeeId()).isEqualTo(employee.getId());
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.employeeNumber()).isEqualTo("EMP-001");
    }

    @Test
    void getOwnProfile_throwsEmployeeNotFoundException_whenNoEmployeeIsLinkedToTheUser() {
        UUID userId = UUID.randomUUID();

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> selfServiceService.getOwnProfile(userId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void getOwnLeave_delegatesToTheLeaveService() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveResponse> page = new PageImpl<>(List.of());

        when(leaveService.getOwnLeave(userId, pageable)).thenReturn(page);

        Page<LeaveResponse> result = selfServiceService.getOwnLeave(userId, pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getOwnAttendance_mapsEachAttendanceRecordToAResponse() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Attendance attendance = attendanceWithId(UUID.randomUUID());
        Page<Attendance> page = new PageImpl<>(List.of(attendance));

        when(attendanceService.getOwnAttendance(userId, pageable)).thenReturn(page);

        Page<AttendanceResponse> result = selfServiceService.getOwnAttendance(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(attendance.getId());
    }

    @Test
    void clockIn_delegatesToAttendanceServiceAndReturnsTheMappedResponse() {
        UUID userId = UUID.randomUUID();
        Attendance attendance = attendanceWithId(UUID.randomUUID());

        when(attendanceService.clockInForUser(userId)).thenReturn(attendance);

        AttendanceResponse response = selfServiceService.clockIn(userId);

        assertThat(response.id()).isEqualTo(attendance.getId());
        verify(attendanceService).clockInForUser(userId);
    }

    @Test
    void clockOut_delegatesToAttendanceServiceAndReturnsTheMappedResponse() {
        UUID userId = UUID.randomUUID();
        Attendance attendance = attendanceWithId(UUID.randomUUID());

        when(attendanceService.clockOutForUser(userId)).thenReturn(attendance);

        AttendanceResponse response = selfServiceService.clockOut(userId);

        assertThat(response.id()).isEqualTo(attendance.getId());
        verify(attendanceService).clockOutForUser(userId);
    }

    @Test
    void getOwnTimesheets_mapsEachTimesheetToAResponse() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Timesheet timesheet = timesheetWithId(UUID.randomUUID());
        Page<Timesheet> page = new PageImpl<>(List.of(timesheet));

        when(timesheetService.getOwnTimesheets(userId, pageable)).thenReturn(page);

        Page<TimesheetResponse> result = selfServiceService.getOwnTimesheets(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(timesheet.getId());
    }

    @Test
    void createTimesheet_delegatesToTimesheetServiceAndReturnsTheMappedResponse() {
        UUID userId = UUID.randomUUID();
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 1, 7);
        Timesheet timesheet = timesheetWithId(UUID.randomUUID());

        when(timesheetService.createForUser(userId, periodStart, periodEnd)).thenReturn(timesheet);

        TimesheetResponse response = selfServiceService.createTimesheet(userId, periodStart, periodEnd);

        assertThat(response.id()).isEqualTo(timesheet.getId());
    }

    @Test
    void addTimesheetEntry_delegatesToTheTimesheetService() {
        UUID userId = UUID.randomUUID();
        UUID timesheetId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 1, 2);
        BigDecimal hoursWorked = BigDecimal.valueOf(8);

        selfServiceService.addTimesheetEntry(userId, timesheetId, workDate, hoursWorked, "Worked on tickets");

        verify(timesheetService).addEntryForUser(
                userId, timesheetId, workDate, hoursWorked, "Worked on tickets");
    }

    @Test
    void submitTimesheet_delegatesToTimesheetServiceAndReturnsTheMappedResponse() {
        UUID userId = UUID.randomUUID();
        Timesheet timesheet = timesheetWithId(UUID.randomUUID());
        timesheet.submit();

        when(timesheetService.submitForUser(userId, timesheet.getId())).thenReturn(timesheet);

        TimesheetResponse response = selfServiceService.submitTimesheet(userId, timesheet.getId());

        assertThat(response.status()).isEqualTo("SUBMITTED");
    }
}
