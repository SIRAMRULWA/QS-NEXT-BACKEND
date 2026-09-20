package za.co.qsnext.employeemanagement.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.calendar.CalendarEvent;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.user.UserService;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private UserService userService;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private CalendarService calendarService;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveService(
                leaveRequestRepository, leaveBalanceRepository, employeeService,
                userService, employeeRepository, notificationPublisher, calendarService);
    }

    private LeaveRequest pendingLeaveRequest(UUID employeeId) {
        LeaveRequest request = new LeaveRequest(
                employeeId, "ANNUAL", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), "Vacation");
        setId(request, UUID.randomUUID());
        return request;
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
        UUID approverId = UUID.randomUUID();

        LeaveRequest request = pendingLeaveRequest(employeeId);
        LeaveBalance balance = new LeaveBalance(employeeId, "ANNUAL", 2026, BigDecimal.TEN);

        when(leaveRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndLeaveYear(employeeId, "ANNUAL", 2026))
                .thenReturn(Optional.of(balance));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        LeaveRequest approved = leaveService.approve(request.getId(), approverId);

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.LEAVE_REQUEST_APPROVED), any(), any());
        verify(calendarService).recordSystemEvent(
                any(), eq(CalendarEvent.TYPE_LEAVE), any(), any(), eq(userId), any());
    }

    @Test
    void approve_throwsAndDoesNotNotify_whenBalanceIsInsufficient() {
        UUID employeeId = UUID.randomUUID();
        LeaveRequest request = pendingLeaveRequest(employeeId);
        LeaveBalance balance = new LeaveBalance(employeeId, "ANNUAL", 2026, BigDecimal.ONE);

        when(leaveRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndLeaveYear(employeeId, "ANNUAL", 2026))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveService.approve(request.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);

        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void approve_throws_whenRequestIsNotPending() {
        UUID employeeId = UUID.randomUUID();
        LeaveRequest request = pendingLeaveRequest(employeeId);
        request.reject();

        when(leaveRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.approve(request.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reject_notifiesTheEmployee_onSuccess() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        LeaveRequest request = pendingLeaveRequest(employeeId);

        when(leaveRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));

        LeaveRequest rejected = leaveService.reject(request.getId());

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        verify(notificationPublisher).publish(
                eq(userId), eq(NotificationType.LEAVE_REQUEST_REJECTED), any(), any());
    }

    @Test
    void reject_throws_whenRequestIsNotPending() {
        UUID employeeId = UUID.randomUUID();
        LeaveRequest request = pendingLeaveRequest(employeeId);
        request.approve(UUID.randomUUID());

        when(leaveRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.reject(request.getId()))
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
