package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import za.co.qsnext.employeemanagement.exception.UnauthorizedException;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @return the id of the currently authenticated user.
     * @throws UnauthorizedException if called outside an authenticated
     *                                request.
     */
    public static UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {

            throw new UnauthorizedException(
                    "No authenticated user found"
            );
        }

        return userDetails.getUserId();
    }
}
