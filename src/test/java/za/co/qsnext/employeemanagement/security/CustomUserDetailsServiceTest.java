package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_buildsUserDetails_withRolesAndPermissionsAsAuthorities() {
        Permission readPermission =
                new Permission(UUID.randomUUID(), "EMPLOYEE_READ", "View employees");
        Role role = new Role(UUID.randomUUID(), "HR_OFFICER", "HR officer");
        setPermissions(role, Set.of(readPermission));

        User user = new User("jane.doe", "jane.doe@qsnext.co.za", "hash");
        user.assignRole(role);
        setId(user, UUID.randomUUID());

        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("jane.doe");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails userDetails = (CustomUserDetails) result;
        assertThat(userDetails.getUserId()).isEqualTo(user.getId());
        assertThat(userDetails.getUsername()).isEqualTo("jane.doe");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
        assertThat(userDetails.getUser()).isEqualTo(user);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_HR_OFFICER", "EMPLOYEE_READ");
    }

    @Test
    void loadUserByUsername_reflectsEnabledAndLockedFlags_whenUserIsDisabledAndLocked() {
        User user = new User("locked.user", "locked.user@qsnext.co.za", "hash");
        user.disable();
        user.lockUntil(OffsetDateTime.now().plusMinutes(15));
        setId(user, UUID.randomUUID());

        when(userRepository.findByUsername("locked.user")).thenReturn(Optional.of(user));

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername("locked.user");

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_reflectsEnabledAndUnlockedFlags_whenUserIsActive() {
        User user = new User("active.user", "active.user@qsnext.co.za", "hash");
        setId(user, UUID.randomUUID());

        when(userRepository.findByUsername("active.user")).thenReturn(Optional.of(user));

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername("active.user");

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
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
