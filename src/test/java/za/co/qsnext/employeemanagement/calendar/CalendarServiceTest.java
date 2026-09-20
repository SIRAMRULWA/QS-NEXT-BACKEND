package za.co.qsnext.employeemanagement.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.calendar.dto.CalendarEventResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.CalendarEventNotFoundException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(calendarEventRepository, employeeRepository);
    }

    @Test
    void createEvent_defaultsToPrivateVisibility() {
        UUID ownerId = UUID.randomUUID();
        when(employeeRepository.findByUserId(ownerId)).thenReturn(Optional.empty());
        when(calendarEventRepository.save(any())).thenAnswer(invocation -> {
            CalendarEvent event = invocation.getArgument(0);
            setId(event, UUID.randomUUID());
            return event;
        });

        OffsetDateTime start = OffsetDateTime.now();
        CalendarEventResponse response = calendarService.createEvent(
                ownerId, "1:1", "Sync", start, start.plusHours(1), false, null);

        assertThat(response.visibility()).isEqualTo(CalendarEvent.VISIBILITY_PRIVATE);
        assertThat(response.eventType()).isEqualTo(CalendarEvent.TYPE_MEETING);
    }

    @Test
    void createEvent_rejectsAnEndTimeBeforeStartTime() {
        OffsetDateTime start = OffsetDateTime.now();

        assertThatThrownBy(() -> calendarService.createEvent(
                UUID.randomUUID(), "Bad", null, start, start.minusHours(1), false, "PRIVATE"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createEvent_rejectsAnInvalidVisibility() {
        OffsetDateTime start = OffsetDateTime.now();

        assertThatThrownBy(() -> calendarService.createEvent(
                UUID.randomUUID(), "Bad", null, start, start.plusHours(1), false, "SECRET"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createEvent_requiresAnEmployeeProfile_forDepartmentVisibility() {
        UUID ownerId = UUID.randomUUID();
        when(employeeRepository.findByUserId(ownerId)).thenReturn(Optional.empty());

        OffsetDateTime start = OffsetDateTime.now();

        assertThatThrownBy(() -> calendarService.createEvent(
                ownerId, "Team sync", null, start, start.plusHours(1), false, "DEPARTMENT"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createHoliday_isPublicAndAllDay() {
        when(calendarEventRepository.save(any())).thenAnswer(invocation -> {
            CalendarEvent event = invocation.getArgument(0);
            setId(event, UUID.randomUUID());
            return event;
        });

        CalendarEventResponse response =
                calendarService.createHoliday("New Year's Day", LocalDate.of(2026, 1, 1));

        assertThat(response.visibility()).isEqualTo(CalendarEvent.VISIBILITY_PUBLIC);
        assertThat(response.eventType()).isEqualTo(CalendarEvent.TYPE_HOLIDAY);
        assertThat(response.allDay()).isTrue();
    }

    @Test
    void listEvents_rejectsAnEndBeforeStart() {
        OffsetDateTime now = OffsetDateTime.now();

        assertThatThrownBy(() ->
                calendarService.listEvents(UUID.randomUUID(), now, now.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteEvent_removesTheEvent_whenRequesterIsTheOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        CalendarEvent event = new CalendarEvent(
                "Meeting", null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                false, CalendarEvent.TYPE_MEETING, CalendarEvent.VISIBILITY_PRIVATE, ownerId, null);
        setId(event, eventId);

        when(calendarEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        calendarService.deleteEvent(eventId, ownerId, false);

        verify(calendarEventRepository).delete(event);
    }

    @Test
    void deleteEvent_isAllowed_forAnAdminWhoIsNotTheOwner() {
        UUID eventId = UUID.randomUUID();
        CalendarEvent event = new CalendarEvent(
                "Meeting", null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                false, CalendarEvent.TYPE_MEETING, CalendarEvent.VISIBILITY_PRIVATE, UUID.randomUUID(), null);
        setId(event, eventId);

        when(calendarEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        calendarService.deleteEvent(eventId, UUID.randomUUID(), true);

        verify(calendarEventRepository).delete(event);
    }

    @Test
    void deleteEvent_isDenied_forANonOwnerNonAdmin() {
        UUID eventId = UUID.randomUUID();
        CalendarEvent event = new CalendarEvent(
                "Meeting", null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                false, CalendarEvent.TYPE_MEETING, CalendarEvent.VISIBILITY_PRIVATE, UUID.randomUUID(), null);
        setId(event, eventId);

        when(calendarEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> calendarService.deleteEvent(eventId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteEvent_throws_whenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        when(calendarEventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.deleteEvent(eventId, UUID.randomUUID(), true))
                .isInstanceOf(CalendarEventNotFoundException.class);
    }

    private static void setId(CalendarEvent event, UUID id) {
        try {
            Field field = CalendarEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
