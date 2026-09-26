package za.co.qsnext.employeemanagement.leave;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.EmployeeAuthorizationService;

import java.util.UUID;

/**
 * Object-level authorization for a single leave request: the same
 * "own record, or an HR/admin role" rule {@link EmployeeAuthorizationService}
 * already enforces for employee records, applied here by first resolving
 * the request's owning employee. A leave request id that doesn't exist is
 * treated as accessible so the controller reaches the service and surfaces
 * its own 404, rather than masking a not-found as a 403 - the same
 * trade-off {@link EmployeeAuthorizationService#canRead} already makes for
 * an unknown employeeId.
 */
@Service("leaveAuthorizationService")
public class LeaveAuthorizationService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeAuthorizationService employeeAuthorizationService;

    public LeaveAuthorizationService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeAuthorizationService employeeAuthorizationService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeAuthorizationService = employeeAuthorizationService;
    }

    public boolean canReadRequest(UUID leaveRequestId, Authentication authentication) {

        return leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequest::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canRead(employeeId, authentication))
                .orElse(true);
    }

    public boolean canActOnRequest(UUID leaveRequestId, Authentication authentication) {

        return leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequest::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canActFor(employeeId, authentication))
                .orElse(true);
    }

    public boolean canApproveRequest(UUID leaveRequestId, Authentication authentication) {

        return leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequest::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canApproveFor(employeeId, authentication))
                .orElse(true);
    }
}
