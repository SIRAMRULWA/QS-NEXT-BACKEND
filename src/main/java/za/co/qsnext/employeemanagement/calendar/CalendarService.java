package za.co.qsnext.employeemanagement.calendar;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.calendar.dto.CalendarEventResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.CalendarEventNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final EmployeeRepository employeeRepository;

    public CalendarService(
            CalendarEventRepository calendarEventRepository,
            EmployeeRepository employeeRepository
    ) {
        this.calendarEventRepository = calendarEventRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<CalendarEventResponse> listEvents(
            UUID userId,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd
    ) {
        if (rangeEnd.isBefore(rangeStart)) {
            throw new BusinessRuleException("Range end cannot be before range start");
        }

        UUID departmentId = employeeRepository.findByUserId(userId)
                .map(Employee::getDepartmentId)
                .orElse(null);

        return calendarEventRepository
                .findVisibleEventsInRange(userId, departmentId, rangeStart, rangeEnd)
                .stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional
    public CalendarEventResponse createEvent(
            UUID ownerUserId,
            String title,
            String description,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            boolean allDay,
            String visibility
    ) {
        validatePeriod(startAt, endAt);

        String safeVisibility = (visibility == null || visibility.isBlank())
                ? CalendarEvent.VISIBILITY_PRIVATE
                : visibility.toUpperCase();

        if (!List.of(
                CalendarEvent.VISIBILITY_PUBLIC,
                CalendarEvent.VISIBILITY_DEPARTMENT,
                CalendarEvent.VISIBILITY_PRIVATE
        ).contains(safeVisibility)) {
            throw new BusinessRuleException("Invalid visibility: " + visibility);
        }

        UUID departmentId = employeeRepository.findByUserId(ownerUserId)
                .map(Employee::getDepartmentId)
                .orElse(null);

        if (CalendarEvent.VISIBILITY_DEPARTMENT.equals(safeVisibility) && departmentId == null) {
            throw new BusinessRuleException(
                    "A department-visible event requires the creator to have an employee profile"
            );
        }

        CalendarEvent event = new CalendarEvent(
                title, description, startAt, endAt, allDay,
                CalendarEvent.TYPE_MEETING, safeVisibility, ownerUserId, departmentId
        );

        return CalendarEventResponse.from(calendarEventRepository.save(event));
    }

    @Transactional
    public CalendarEventResponse createHoliday(String title, LocalDate date) {

        OffsetDateTime startAt = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endAt = date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        CalendarEvent event = new CalendarEvent(
                title, null, startAt, endAt, true,
                CalendarEvent.TYPE_HOLIDAY, CalendarEvent.VISIBILITY_PUBLIC, null, null
        );

        return CalendarEventResponse.from(calendarEventRepository.save(event));
    }

    /**
     * Called by other modules (leave approval, shift assignment) to
     * reflect a business event on the calendar, rather than as a direct
     * user-facing action.
     */
    @Transactional
    public void recordSystemEvent(
            String title,
            String eventType,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            UUID ownerUserId,
            UUID departmentId
    ) {
        calendarEventRepository.save(new CalendarEvent(
                title, null, startAt, endAt, true,
                eventType, CalendarEvent.VISIBILITY_PRIVATE, ownerUserId, departmentId
        ));
    }

    @Transactional
    public void deleteEvent(UUID eventId, UUID requesterUserId, boolean requesterIsAdmin) {

        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() ->
                        new CalendarEventNotFoundException("Calendar event not found: " + eventId)
                );

        if (!requesterIsAdmin
                && (event.getOwnerUserId() == null || !event.getOwnerUserId().equals(requesterUserId))) {
            throw new AccessDeniedException("Only the event owner or an administrator can delete this event");
        }

        calendarEventRepository.delete(event);
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (endAt.isBefore(startAt)) {
            throw new BusinessRuleException("Event end time cannot be before start time");
        }
    }
}
