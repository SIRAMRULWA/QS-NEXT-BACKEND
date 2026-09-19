package za.co.qsnext.employeemanagement.security;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.user.Permission;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loaded on every authenticated request (JwtAuthenticationFilter) as well
 * as on login (DaoAuthenticationProvider), so the result is cached in
 * Redis. The cache entry must never outlive a change to the user's
 * enabled state, password, or role/permission assignment; every service
 * method that can change those calls {@link #evictUser(String)}
 * immediately afterwards. The cache also carries a short TTL as a safety
 * net in case an eviction is ever missed.
 */
@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "userDetails", key = "#username")
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                authorityNames(user)
        );
    }

    @CacheEvict(value = "userDetails", key = "#username")
    public void evictUser(String username) {
        // Cache eviction only; nothing else to do.
    }

    private Set<String> authorityNames(User user) {

        Set<Role> roles = user.getRoles();

        Stream<String> roleAuthorities =
                roles.stream()
                        .map(role -> "ROLE_" + role.getName());

        Stream<String> permissionAuthorities =
                roles.stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName);

        return Stream.concat(roleAuthorities, permissionAuthorities)
                .collect(Collectors.toUnmodifiableSet());
    }
}