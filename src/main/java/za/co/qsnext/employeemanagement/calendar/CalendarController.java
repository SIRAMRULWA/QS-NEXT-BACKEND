package za.co.qsnext.employeemanagement.calendar;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.calendar.dto.CalendarEventResponse;
import za.co.qsnext.employeemanagement.calendar.dto.CreateCalendarEventRequest;
import za.co.qsnext.employeemanagement.calendar.dto.CreateHolidayRequest;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calendar", description = "Company and personal calendar events and holidays.")
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PreAuthorize("hasAuthority('CALENDAR_READ')")
    @Operation(summary = "List events")
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> listEvents(
            Authentication authentication,
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                calendarService.listEvents(userDetails.getUserId(), start, end)
        );
    }

    @PreAuthorize("hasAuthority('CALENDAR_CREATE')")
    @Operation(summary = "Create event")
    @PostMapping("/events")
    public ResponseEntity<CalendarEventResponse> createEvent(
            Authentication authentication,
            @Valid @RequestBody CreateCalendarEventRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        CalendarEventResponse response = calendarService.createEvent(
                userDetails.getUserId(),
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.allDay(),
                request.visibility()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('CALENDAR_MANAGE_HOLIDAYS')")
    @Operation(summary = "Create holiday")
    @PostMapping("/holidays")
    public ResponseEntity<CalendarEventResponse> createHoliday(
            @Valid @RequestBody CreateHolidayRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                calendarService.createHoliday(request.title(), request.date())
        );
    }

    @PreAuthorize("hasAuthority('CALENDAR_CREATE')")
    @Operation(summary = "Delete event")
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            Authentication authentication,
            @PathVariable UUID eventId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));

        calendarService.deleteEvent(eventId, userDetails.getUserId(), isAdmin);

        return ResponseEntity.noContent().build();
    }
}
