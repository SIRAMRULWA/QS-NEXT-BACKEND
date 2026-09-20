package za.co.qsnext.employeemanagement.security;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * The subset of a user's data needed to authorize a request, cached in
 * Redis keyed by username so {@link JwtAuthenticationFilter} does not hit
 * the database on every authenticated request. Deliberately excludes the
 * password hash: nothing about request authorization needs it (the JWT
 * itself is the proof of authentication), and it should not sit in a
 * shared cache unnecessarily.
 */
public record CachedUserPrincipal(
        UUID userId,
        String username,
        boolean enabled,
        boolean accountNonLocked,
        Set<String> authorities
) implements Serializable {
}
