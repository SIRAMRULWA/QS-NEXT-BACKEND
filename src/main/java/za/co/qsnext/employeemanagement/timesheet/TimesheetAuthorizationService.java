package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.EmployeeAuthorizationService;

import java.util.UUID;

/**
 * Object-level authorization for a single timesheet: the same "own record,
 * or an HR/admin role" rule {@link EmployeeAuthorizationService} already
 * enforces for employee records, applied here by first resolving the
 * timesheet's owning employee. A timesheet id that doesn't exist is
 * treated as accessible so the controller reaches the service and surfaces
 * its own 404, rather than masking a not-found as a 403 - the same
 * trade-off {@link EmployeeAuthorizationService#canRead} already makes for
 * an unknown employeeId.
 */
@Service("timesheetAuthorizationService")
public class TimesheetAuthorizationService {

    private final TimesheetRepository timesheetRepository;
    private final EmployeeAuthorizationService employeeAuthorizationService;

    public TimesheetAuthorizationService(
            TimesheetRepository timesheetRepository,
            EmployeeAuthorizationService employeeAuthorizationService
    ) {
        this.timesheetRepository = timesheetRepository;
        this.employeeAuthorizationService = employeeAuthorizationService;
    }

    public boolean canReadTimesheet(UUID timesheetId, Authentication authentication) {

        return timesheetRepository.findById(timesheetId)
                .map(Timesheet::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canRead(employeeId, authentication))
                .orElse(true);
    }

    public boolean canActOnTimesheet(UUID timesheetId, Authentication authentication) {

        return timesheetRepository.findById(timesheetId)
                .map(Timesheet::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canActFor(employeeId, authentication))
                .orElse(true);
    }

    public boolean canApproveTimesheet(UUID timesheetId, Authentication authentication) {

        return timesheetRepository.findById(timesheetId)
                .map(Timesheet::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canApproveFor(employeeId, authentication))
                .orElse(true);
    }
}
