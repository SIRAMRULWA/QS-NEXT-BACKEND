package za.co.qsnext.employeemanagement.security;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.config.RedisCacheNames;
import za.co.qsnext.employeemanagement.user.Permission;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Redis-cached lookup of the data {@link JwtAuthenticationFilter} needs to
 * authorize a request. This is what makes JWT-based authentication not
 * cost a database round trip on every single API call: without it,
 * {@code CustomUserDetailsService.loadUserByUsername} (roles + permissions,
 * via an entity graph) would run once per request rather than once per
 * cache TTL window.
 */
@Service
public class UserAuthorizationCacheService {

    private final UserRepository userRepository;

    public UserAuthorizationCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = RedisCacheNames.USER_PRINCIPALS, key = "#username")
    public Optional<CachedUserPrincipal> findPrincipal(String username) {
        return userRepository.findByUsername(username)
                .map(UserAuthorizationCacheService::toPrincipal);
    }

    @CacheEvict(cacheNames = RedisCacheNames.USER_PRINCIPALS, key = "#username")
    public void evict(String username) {
        // Cache entry removed by Spring's caching aspect.
    }

    private static CachedUserPrincipal toPrincipal(User user) {

        Set<Role> roles = user.getRoles();

        Stream<String> roleAuthorities =
                roles.stream().map(role -> "ROLE_" + role.getName());

        Stream<String> permissionAuthorities =
                roles.stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName);

        Set<String> authorities = Stream.concat(roleAuthorities, permissionAuthorities)
                .collect(Collectors.toUnmodifiableSet());

        return new CachedUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                !user.isLocked(),
                authorities
        );
    }
}
