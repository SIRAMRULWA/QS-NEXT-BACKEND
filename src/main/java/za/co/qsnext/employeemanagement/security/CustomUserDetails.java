package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import za.co.qsnext.employeemanagement.user.Permission;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Two ways to build this: from a full {@link User} entity (the login path,
 * via {@link CustomUserDetailsService}, which needs the password hash for
 * credential matching), or from a {@link CachedUserPrincipal} (the
 * per-request authorization path, via {@link JwtAuthenticationFilter},
 * which is backed by a Redis cache and deliberately never carries a
 * password hash). {@link #getUser()} is only populated in the former case.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.userId = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.isEnabled();
        this.accountNonLocked = !user.isLocked();
        this.authorities = computeAuthorities(user);
    }

    public CustomUserDetails(CachedUserPrincipal principal) {
        this.user = null;
        this.userId = principal.userId();
        this.username = principal.username();
        this.passwordHash = null;
        this.enabled = principal.enabled();
        this.accountNonLocked = principal.accountNonLocked();
        this.authorities = principal.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<GrantedAuthority> computeAuthorities(User user) {

        Set<Role> roles = user.getRoles();

        Stream<GrantedAuthority> roleAuthorities =
                roles.stream()
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.getName()
                                )
                        );

        Stream<GrantedAuthority> permissionAuthorities =
                roles.stream()
                        .flatMap(role ->
                                role.getPermissions().stream()
                        )
                        .map(Permission::getName)
                        .map(SimpleGrantedAuthority::new);

        return Stream.concat(
                        roleAuthorities,
                        permissionAuthorities
                )
                .collect(Collectors.toUnmodifiableSet());
    }

    public UUID getUserId() {
        return userId;
    }

    /**
     * The full user entity. Only populated when this instance was built
     * from the login path ({@link CustomUserDetailsService}); {@code null}
     * when built from the cached, per-request authorization path.
     */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return accountNonLocked;
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
