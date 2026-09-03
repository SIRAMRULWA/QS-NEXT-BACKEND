package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

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
}