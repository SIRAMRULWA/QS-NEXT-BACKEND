package za.co.qsnext.employeemanagement.mobile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.attendance.Attendance;
import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.calendar.dto.CalendarEventResponse;
import za.co.qsnext.employeemanagement.exception.MobileDeviceNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveService;
import za.co.qsnext.employeemanagement.leave.dto.LeaveBalanceResponse;
import za.co.qsnext.employeemanagement.mobile.dto.MobileDashboardResponse;
import za.co.qsnext.employeemanagement.mobile.dto.MobileDeviceResponse;
import za.co.qsnext.employeemanagement.notification.NotificationService;
import za.co.qsnext.employeemanagement.notification.dto.NotificationResponse;
import za.co.qsnext.employeemanagement.selfservice.SelfServiceService;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileServiceTest {

    @Mock
    private SelfServiceService selfServiceService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private LeaveService leaveService;
    @Mock
    private CalendarService calendarService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MobileDeviceRepository mobileDeviceRepository;
    @Mock
    private AuditService auditService;

    private MobileService mobileService;

    @BeforeEach
    void setUp() {
        mobileService = new MobileService(
                selfServiceService, attendanceService, leaveService, calendarService,
                notificationService, mobileDeviceRepository, auditService);
    }

    @Test
    void getDashboard_composesEveryDomainServiceIntoOneResponse() {
        UUID userId = UUID.randomUUID();

        SelfServiceProfileResponse profile = new SelfServiceProfileResponse(
                UUID.randomUUID(), userId, UUID.randomUUID(), "EMP-1", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1), "ACTIVE");
        when(selfServiceService.getOwnProfile(userId)).thenReturn(profile);

        Attendance attendance = new Attendance(UUID.randomUUID(), LocalDate.now());
        when(attendanceService.getOwnAttendanceForDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        List<LeaveBalanceResponse> balances = List.of(new LeaveBalanceResponse(
                UUID.randomUUID(), UUID.randomUUID(), "ANNUAL", 2026,
                java.math.BigDecimal.valueOf(21), java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(21),
                OffsetDateTime.now(), OffsetDateTime.now(), 0L));
        when(leaveService.getOwnLeaveBalances(eq(userId), anyInt())).thenReturn(balances);

        List<CalendarEventResponse> events = List.of(new CalendarEventResponse(
                UUID.randomUUID(), "Team meeting", null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                false, "MEETING", "PUBLIC", userId, null));
        when(calendarService.listEvents(eq(userId), any(), any())).thenReturn(events);

        Page<NotificationResponse> unreadPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 3);
        when(notificationService.getUnread(eq(userId), any(PageRequest.class))).thenReturn(unreadPage);

        MobileDashboardResponse response = mobileService.getDashboard(userId);

        assertThat(response.profile()).isEqualTo(profile);
        assertThat(response.todayAttendance()).isNotNull();
        assertThat(response.leaveBalances()).isEqualTo(balances);
        assertThat(response.upcomingEvents()).isEqualTo(events);
        assertThat(response.unreadNotificationCount()).isEqualTo(3);
    }

    @Test
    void getDashboard_toleratesNoAttendanceRecordForToday() {
        UUID userId = UUID.randomUUID();

        when(selfServiceService.getOwnProfile(userId)).thenReturn(new SelfServiceProfileResponse(
                UUID.randomUUID(), userId, UUID.randomUUID(), "EMP-1", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1), "ACTIVE"));
        when(attendanceService.getOwnAttendanceForDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(leaveService.getOwnLeaveBalances(eq(userId), anyInt())).thenReturn(List.of());
        when(calendarService.listEvents(eq(userId), any(), any())).thenReturn(List.of());
        when(notificationService.getUnread(eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        MobileDashboardResponse response = mobileService.getDashboard(userId);

        assertThat(response.todayAttendance()).isNull();
    }

    @Test
    void registerDevice_createsANewDeviceForAnUnknownToken() {
        UUID userId = UUID.randomUUID();

        when(mobileDeviceRepository.findByDeviceToken("token-1")).thenReturn(Optional.empty());
        when(mobileDeviceRepository.save(any(MobileDevice.class))).thenAnswer(invocation -> {
            MobileDevice device = invocation.getArgument(0);
            setId(device, UUID.randomUUID());
            return device;
        });

        MobileDeviceResponse response = mobileService.registerDevice(userId, "token-1", "IOS");

        assertThat(response.platform()).isEqualTo("IOS");
    }

    @Test
    void registerDevice_reassignsAnExistingTokenToTheNewOwner() {
        UUID oldOwner = UUID.randomUUID();
        UUID newOwner = UUID.randomUUID();

        MobileDevice existing = new MobileDevice(oldOwner, "token-1", "ANDROID");
        setId(existing, UUID.randomUUID());

        when(mobileDeviceRepository.findByDeviceToken("token-1")).thenReturn(Optional.of(existing));
        when(mobileDeviceRepository.save(existing)).thenReturn(existing);

        mobileService.registerDevice(newOwner, "token-1", "IOS");

        assertThat(existing.getUserId()).isEqualTo(newOwner);
        assertThat(existing.getPlatform()).isEqualTo("IOS");
    }

    @Test
    void unregisterDevice_throws_whenDeviceDoesNotExist() {
        UUID deviceId = UUID.randomUUID();
        when(mobileDeviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mobileService.unregisterDevice(UUID.randomUUID(), deviceId))
                .isInstanceOf(MobileDeviceNotFoundException.class);
    }

    @Test
    void unregisterDevice_isDenied_forANonOwner() {
        UUID deviceId = UUID.randomUUID();
        MobileDevice device = new MobileDevice(UUID.randomUUID(), "token-1", "IOS");
        setId(device, deviceId);

        when(mobileDeviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> mobileService.unregisterDevice(UUID.randomUUID(), deviceId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unregisterDevice_deletesTheDevice_forItsOwner() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        MobileDevice device = new MobileDevice(userId, "token-1", "IOS");
        setId(device, deviceId);

        when(mobileDeviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        mobileService.unregisterDevice(userId, deviceId);

        org.mockito.Mockito.verify(mobileDeviceRepository).delete(device);
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
