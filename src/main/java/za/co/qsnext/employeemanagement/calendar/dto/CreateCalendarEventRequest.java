package za.co.qsnext.employeemanagement.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateCalendarEventRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Start time is required")
        OffsetDateTime startAt,

        @NotNull(message = "End time is required")
        OffsetDateTime endAt,

        boolean allDay,

        /**
         * PUBLIC, DEPARTMENT or PRIVATE. Defaults to PRIVATE (visible only
         * to the creator) when not supplied.
         */
        String visibility
) {
}
