package za.co.qsnext.employeemanagement.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * The current authenticated caller's identity and resolved authorization
 * grants - the only source of truth a frontend has for its own roles and
 * permissions, since the JWT itself carries neither (see
 * {@code AuthService#getCurrentUser}). {@code employeeId} is null for a
 * user with no linked {@code Employee} record yet (e.g. freshly
 * registered, not yet onboarded).
 */
public record MeResponse(
        UUID userId,
        String username,
        String email,
        boolean emailVerified,
        UUID employeeId,
        List<String> roles,
        List<String> authorities
) {
}
