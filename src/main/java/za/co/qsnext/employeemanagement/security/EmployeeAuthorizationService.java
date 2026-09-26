package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;

import java.util.List;
import java.util.UUID;

@Service("employeeAuthorizationService")
public class EmployeeAuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HR_MANAGER = "ROLE_HR_MANAGER";
    private static final String ROLE_HR_OFFICER = "ROLE_HR_OFFICER";

    private final EmployeeRepository employeeRepository;

    public EmployeeAuthorizationService(
            EmployeeRepository employeeRepository
    ) {
        this.employeeRepository = employeeRepository;
    }

    public boolean canRead(
            UUID employeeId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        if (canManage(authentication)) {
            return true;
        }

        CustomUserDetails userDetails =
                getUserDetails(authentication);

        if (userDetails == null) {
            return false;
        }

        UUID currentUserId =
                userDetails.getUserId();

        return employeeRepository
                .findById(employeeId)
                .map(employee ->
                        currentUserId.equals(employee.getUserId())
                                || isDirectManagerOf(employee, currentUserId)
                )
                .orElse(false);
    }

    /**
     * True when the caller may act on this employee's own records
     * (request leave, create a timesheet, clock in): the employee
     * themselves, or HR/admin. Unlike {@link #canRead}, a line manager
     * does NOT qualify - a manager approves their reports' requests but
     * never files them on their behalf.
     */
    public boolean canActFor(
            UUID employeeId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        if (canManage(authentication)) {
            return true;
        }

        UUID currentUserId = currentUserId(authentication);

        if (currentUserId == null || employeeId == null) {
            return false;
        }

        return employeeRepository
                .findById(employeeId)
                .map(Employee::getUserId)
                .map(currentUserId::equals)
                .orElse(false);
    }

    /**
     * True when the caller may approve or reject this employee's leave,
     * timesheets or expense claims: HR/admin for anyone, or the
     * employee's direct manager. Nobody who isn't HR/admin can approve
     * their own requests.
     */
    public boolean canApproveFor(
            UUID employeeId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        if (canManage(authentication)) {
            return true;
        }

        UUID currentUserId = currentUserId(authentication);

        if (currentUserId == null || employeeId == null) {
            return false;
        }

        return employeeRepository
                .findById(employeeId)
                .map(employee -> isDirectManagerOf(employee, currentUserId))
                .orElse(false);
    }

    /**
     * The employee ids of the caller's direct reports; empty for anyone
     * without an employee record or without reports.
     */
    public List<UUID> directReportIds(Authentication authentication) {

        UUID currentUserId = currentUserId(authentication);

        if (currentUserId == null) {
            return List.of();
        }

        return employeeRepository.findByUserId(currentUserId)
                .map(manager -> employeeRepository.findByManagerId(manager.getId())
                        .stream()
                        .map(Employee::getId)
                        .toList())
                .orElse(List.of());
    }

    private boolean isDirectManagerOf(
            Employee employee,
            UUID currentUserId
    ) {

        UUID managerId = employee.getManagerId();

        if (managerId == null
                || currentUserId.equals(employee.getUserId())) {
            return false;
        }

        return employeeRepository.findById(managerId)
                .map(Employee::getUserId)
                .map(currentUserId::equals)
                .orElse(false);
    }

    private UUID currentUserId(Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        CustomUserDetails userDetails = getUserDetails(authentication);

        return userDetails == null ? null : userDetails.getUserId();
    }

    /**
     * True for a caller in an HR/admin role that isn't scoped to their own
     * employee record - i.e. someone who's supposed to see everyone's data,
     * not just their own. Used both as the elevated-access bypass in
     * {@link #canRead} and, on its own, to gate company-wide list endpoints
     * (e.g. "every leave request with status X") that have no single
     * employeeId to check ownership of.
     */
    public boolean canManage(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        return hasAuthority(authentication, ROLE_ADMIN)
                || hasAuthority(authentication, ROLE_HR_MANAGER)
                || hasAuthority(authentication, ROLE_HR_OFFICER);
    }

    private boolean hasAuthority(
            Authentication authentication,
            String authority
    ) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(
                        grantedAuthority ->
                                authority.equals(
                                        grantedAuthority.getAuthority()
                                )
                );
    }

    private CustomUserDetails getUserDetails(
            Authentication authentication
    ) {

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        return null;
    }
}