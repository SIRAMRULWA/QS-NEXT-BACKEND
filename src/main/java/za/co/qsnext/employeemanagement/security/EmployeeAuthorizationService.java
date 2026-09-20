package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;

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
                .map(Employee::getUserId)
                .map(currentUserId::equals)
                .orElse(false);
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