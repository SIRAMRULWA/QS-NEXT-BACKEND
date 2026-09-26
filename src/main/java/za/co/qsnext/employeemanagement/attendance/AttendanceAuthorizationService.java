package za.co.qsnext.employeemanagement.attendance;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.EmployeeAuthorizationService;

import java.util.UUID;

/**
 * Object-level authorization for a single attendance record: the same
 * "own record, or an HR/admin role" rule {@link EmployeeAuthorizationService}
 * already enforces for employee records, applied here by first resolving
 * the record's owning employee. An attendance id that doesn't exist is
 * treated as accessible so the controller reaches the service and surfaces
 * its own 404, rather than masking a not-found as a 403 - the same
 * trade-off {@link EmployeeAuthorizationService#canRead} already makes for
 * an unknown employeeId.
 */
@Service("attendanceAuthorizationService")
public class AttendanceAuthorizationService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeAuthorizationService employeeAuthorizationService;

    public AttendanceAuthorizationService(
            AttendanceRepository attendanceRepository,
            EmployeeAuthorizationService employeeAuthorizationService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.employeeAuthorizationService = employeeAuthorizationService;
    }

    public boolean canReadRecord(UUID attendanceId, Authentication authentication) {

        return attendanceRepository.findById(attendanceId)
                .map(Attendance::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canRead(employeeId, authentication))
                .orElse(true);
    }

    public boolean canActOnRecord(UUID attendanceId, Authentication authentication) {

        return attendanceRepository.findById(attendanceId)
                .map(Attendance::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canActFor(employeeId, authentication))
                .orElse(true);
    }
}
