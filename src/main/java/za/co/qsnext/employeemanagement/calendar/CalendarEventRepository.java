package za.co.qsnext.employeemanagement.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    /**
     * Events overlapping [rangeStart, rangeEnd] that {@code userId} (in
     * {@code departmentId}) is allowed to see: PUBLIC events, DEPARTMENT
     * events for their own department, and PRIVATE events they own.
     */
    @Query("""
            select e from CalendarEvent e
            where e.startAt <= :rangeEnd
            and e.endAt >= :rangeStart
            and (
                e.visibility = 'PUBLIC'
                or (e.visibility = 'DEPARTMENT' and e.departmentId = :departmentId)
                or (e.visibility = 'PRIVATE' and e.ownerUserId = :userId)
            )
            order by e.startAt asc
            """)
    List<CalendarEvent> findVisibleEventsInRange(
            @Param("userId") UUID userId,
            @Param("departmentId") UUID departmentId,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );
}
