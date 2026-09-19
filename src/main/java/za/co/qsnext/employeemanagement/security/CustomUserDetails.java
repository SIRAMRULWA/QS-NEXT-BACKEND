package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A flat, JPA-independent authentication principal. Roles/permissions are
 * pre-flattened into {@code authorityNames} at load time rather than
 * carrying live {@code User}/{@code Role}/{@code Permission} entities, so
 * that this type is safely and predictably cacheable in Redis: as a
 * record backed only by concrete JDK types, Jackson can (de)serialize it
 * with no custom configuration and no risk of touching a Hibernate lazy
 * collection outside its session (see CustomUserDetailsService, which is
 * the only place this is constructed).
 */
public record CustomUserDetails(
        UUID userId,
        String username,
        String email,
        String passwordHash,
        boolean enabled,
        Set<String> authorityNames
) implements UserDetails {

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
