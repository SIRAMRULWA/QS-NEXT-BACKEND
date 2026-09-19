package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Access to the currently authenticated principal outside of a
 * {@code @PreAuthorize}/{@code @AuthenticationPrincipal} context, e.g. from
 * services that need to know "who is doing this" for auditing purposes.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CustomUserDetails> currentUserDetails() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            return Optional.empty();
        }

        return Optional.of(userDetails);
    }

    public static Optional<UUID> currentUserId() {
        return currentUserDetails().map(CustomUserDetails::getUserId);
    }

    public static Optional<String> currentUsername() {
        return currentUserDetails().map(CustomUserDetails::getUsername);
    }
}
