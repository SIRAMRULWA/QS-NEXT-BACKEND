package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetEntryRepository
        extends JpaRepository<TimesheetEntry, UUID> {

    List<TimesheetEntry> findByTimesheetIdOrderByWorkDateAsc(
            UUID timesheetId
    );

    Optional<TimesheetEntry> findByTimesheetIdAndWorkDate(
            UUID timesheetId,
            LocalDate workDate
    );

    @Query("""
            select te.timesheetId as timesheetId, coalesce(sum(te.hoursWorked), 0) as totalHours
            from TimesheetEntry te
            where te.timesheetId in :timesheetIds
            group by te.timesheetId
            """)
    List<TimesheetHoursSummary> sumHoursGroupedByTimesheetId(@Param("timesheetIds") List<UUID> timesheetIds);
}