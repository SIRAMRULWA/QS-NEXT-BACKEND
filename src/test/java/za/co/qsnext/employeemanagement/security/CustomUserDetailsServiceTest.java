package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-level coverage of the role-flattening logic, isolated from Redis
 * caching (the {@code @Cacheable} annotation has no effect when the bean
 * is constructed directly rather than through a Spring proxy). Full
 * caching/eviction behavior is covered separately by an integration test
 * against real Redis.
 */
class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(userRepository);

    @Test
    void loadsAndFlattensRoleIntoAPrefixedAuthority() {

        Role role = new Role(
                UUID.randomUUID(),
                "EMPLOYEE",
                "Standard employee"
        );

        User user = new User(
                "jdoe",
                "jdoe@example.com",
                "hashed-password"
        );
        user.assignRole(role);

        when(userRepository.findByUsername("jdoe"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("jdoe");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("jdoe");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void disabledUserIsReflectedInTheReturnedPrincipal() {

        User user = new User(
                "disabled_user",
                "disabled@example.com",
                "hashed-password"
        );
        user.disable();

        when(userRepository.findByUsername("disabled_user"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("disabled_user");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void throwsWhenUserNotFound() {

        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
