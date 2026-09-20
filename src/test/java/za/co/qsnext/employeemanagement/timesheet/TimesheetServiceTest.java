package za.co.qsnext.employeemanagement.timesheet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.user.UserService;

import java.lang.reflect.Field;
import java.time.LocalDate;
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
class TimesheetServiceTest {

    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private UserService userService;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;

    private TimesheetService timesheetService;

    @BeforeEach
    void setUp() {
        timesheetService = new TimesheetService(
                timesheetRepository, timesheetEntryRepository, employeeService,
                userService, employeeRepository, notificationPublisher);
    }

    private Timesheet submittedTimesheet(UUID employeeId) {
        Timesheet timesheet = new Timesheet(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));
        setId(timesheet, UUID.randomUUID());
        timesheet.submit();
        return timesheet;
    }

    private Employee employeeWithId(UUID employeeId, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-001", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, employeeId);
        return employee;
    }

    @Test
    void approve_notifiesTheEmployee_onSuccess() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Timesheet timesheet = submittedTimesheet(employeeId);

        when(timesheetRepository.findById(timesheet.getId())).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.saveAndFlush(timesheet)).thenReturn(timesheet);
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        Timesheet approved = timesheetService.approve(timesheet.getId(), UUID.randomUUID());

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.TIMESHEET_APPROVED), any(), any());
    }

    @Test
    void approve_throws_whenTimesheetIsNotSubmitted() {
        UUID employeeId = UUID.randomUUID();
        Timesheet timesheet = new Timesheet(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));
        setId(timesheet, UUID.randomUUID());

        when(timesheetRepository.findById(timesheet.getId())).thenReturn(Optional.of(timesheet));

        assertThatThrownBy(() -> timesheetService.approve(timesheet.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);

        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void reject_notifiesTheEmployee_onSuccess() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Timesheet timesheet = submittedTimesheet(employeeId);

        when(timesheetRepository.findById(timesheet.getId())).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.saveAndFlush(timesheet)).thenReturn(timesheet);
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        Timesheet rejected = timesheetService.reject(timesheet.getId());

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.TIMESHEET_REJECTED), any(), any());
    }

    @Test
    void reject_throws_whenTimesheetIsNotSubmitted() {
        UUID employeeId = UUID.randomUUID();
        Timesheet timesheet = new Timesheet(
                employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));
        setId(timesheet, UUID.randomUUID());

        when(timesheetRepository.findById(timesheet.getId())).thenReturn(Optional.of(timesheet));

        assertThatThrownBy(() -> timesheetService.reject(timesheet.getId()))
                .isInstanceOf(BusinessRuleException.class);

        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
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
