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

        if (hasAuthority(authentication, ROLE_ADMIN)
                || hasAuthority(authentication, ROLE_HR_MANAGER)
                || hasAuthority(authentication, ROLE_HR_OFFICER)) {
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