package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.user.Permission;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The {@code @Cacheable}/{@code @CacheEvict} annotations on this service
 * only take effect through Spring's caching proxy, which a plain unit test
 * does not exercise - that behaviour is covered by the AuthenticationIntegrationTest
 * lockout/disable scenarios instead. This test covers the mapping logic:
 * that the cached principal ends up with exactly the same authority set
 * CustomUserDetails would have computed directly from the entity.
 */
@ExtendWith(MockitoExtension.class)
class UserAuthorizationCacheServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserAuthorizationCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new UserAuthorizationCacheService(userRepository);
    }

    @Test
    void findPrincipal_mapsRolesAndPermissionsToAuthorityStrings() {
        Permission readPermission =
                new Permission(UUID.randomUUID(), "EMPLOYEE_READ", "View employees");
        Permission createPermission =
                new Permission(UUID.randomUUID(), "EMPLOYEE_CREATE", "Create employees");

        Role role = new Role(UUID.randomUUID(), "HR_OFFICER", "HR officer");
        setPermissions(role, Set.of(readPermission, createPermission));

        User user = new User("jane.doe", "jane.doe@qsnext.co.za", "hash");
        user.assignRole(role);
        setId(user, UUID.randomUUID());

        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        Optional<CachedUserPrincipal> result = cacheService.findPrincipal("jane.doe");

        assertThat(result).isPresent();
        CachedUserPrincipal principal = result.get();
        assertThat(principal.userId()).isEqualTo(user.getId());
        assertThat(principal.username()).isEqualTo("jane.doe");
        assertThat(principal.enabled()).isTrue();
        assertThat(principal.accountNonLocked()).isTrue();
        assertThat(principal.authorities()).containsExactlyInAnyOrder(
                "ROLE_HR_OFFICER", "EMPLOYEE_READ", "EMPLOYEE_CREATE");
    }

    @Test
    void findPrincipal_reflectsLockedState() {
        User user = new User("locked.user", "locked.user@qsnext.co.za", "hash");
        user.lockUntil(OffsetDateTime.now().plusMinutes(15));
        setId(user, UUID.randomUUID());

        when(userRepository.findByUsername("locked.user")).thenReturn(Optional.of(user));

        CachedUserPrincipal principal = cacheService.findPrincipal("locked.user").orElseThrow();

        assertThat(principal.accountNonLocked()).isFalse();
    }

    @Test
    void findPrincipal_returnsEmpty_whenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(cacheService.findPrincipal("ghost")).isEmpty();
    }

    private static void setId(User user, UUID id) {
        setField(user, User.class, "id", id);
    }

    private static void setPermissions(Role role, Set<Permission> permissions) {
        setField(role, Role.class, "permissions", permissions);
    }

    private static void setField(Object target, Class<?> declaringClass, String fieldName, Object value) {
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
