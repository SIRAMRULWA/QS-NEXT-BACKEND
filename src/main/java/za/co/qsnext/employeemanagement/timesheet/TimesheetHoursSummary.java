package za.co.qsnext.employeemanagement.timesheet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data interface projection for the per-timesheet total-hours
 * aggregate query used by the Reporting module, avoiding an N+1 fetch
 * of every entry for every timesheet in a report.
 */
public interface TimesheetHoursSummary {

    UUID getTimesheetId();

    BigDecimal getTotalHours();
}
