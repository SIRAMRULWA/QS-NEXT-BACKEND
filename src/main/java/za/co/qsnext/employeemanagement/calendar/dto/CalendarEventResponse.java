package za.co.qsnext.employeemanagement.calendar.dto;

import za.co.qsnext.employeemanagement.calendar.CalendarEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        String title,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean allDay,
        String eventType,
        String visibility,
        UUID ownerUserId,
        UUID departmentId
) {

    public static CalendarEventResponse from(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isAllDay(),
                event.getEventType(),
                event.getVisibility(),
                event.getOwnerUserId(),
                event.getDepartmentId()
        );
    }
}
