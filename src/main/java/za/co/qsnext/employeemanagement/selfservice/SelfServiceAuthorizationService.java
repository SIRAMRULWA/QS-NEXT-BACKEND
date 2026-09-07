package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.UUID;

@Service("selfServiceAuthorizationService")
public class SelfServiceAuthorizationService {

    public boolean canAccessOwnProfile(
            UUID userId,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return userDetails
                .getUserId()
                .equals(userId);
    }
}