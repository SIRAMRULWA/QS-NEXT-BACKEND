package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAuthorizationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new EmployeeAuthorizationService(employeeRepository);
    }

    private Employee employeeOwnedBy(UUID userId) {
        Employee employee = new Employee(
                userId,
                UUID.randomUUID(),
                "EMP-001",
                "Jane",
                "Doe",
                "0123456789",
                "Engineer",
                LocalDate.now());
        setId(employee, UUID.randomUUID());
        return employee;
    }

    private Authentication authenticatedAs(UUID userId, String... roles) {
        CustomUserDetails principal =
                new CustomUserDetails(
                        new CachedUserPrincipal(userId, "jane.doe", true, true, Set.of()));

        List<SimpleGrantedAuthority> authorities =
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void canRead_returnsFalse_whenAuthenticationIsNull() {
        assertThat(authorizationService.canRead(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    void canRead_returnsFalse_whenAuthenticationIsNotAuthenticated() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("jane.doe", "credentials");

        assertThat(authorizationService.canRead(UUID.randomUUID(), authentication)).isFalse();
    }

    @Test
    void canRead_returnsTrue_whenEmployeeIsOwnedByCurrentUser() {
        UUID userId = UUID.randomUUID();
        Employee employee = employeeOwnedBy(userId);
        Authentication authentication = authenticatedAs(userId);

        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThat(authorizationService.canRead(employee.getId(), authentication)).isTrue();
    }

    @Test
    void canRead_returnsFalse_whenEmployeeIsNotOwnedAndUserHasNoQualifyingRole() {
        UUID currentUserId = UUID.randomUUID();
        Employee employee = employeeOwnedBy(UUID.randomUUID());
        Authentication authentication = authenticatedAs(currentUserId);

        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThat(authorizationService.canRead(employee.getId(), authentication)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_HR_OFFICER"})
    void canRead_returnsTrue_whenUserHasQualifyingRole_evenWhenNotOwner(String role) {
        Employee employee = employeeOwnedBy(UUID.randomUUID());
        Authentication authentication = authenticatedAs(UUID.randomUUID(), role);

        assertThat(authorizationService.canRead(employee.getId(), authentication)).isTrue();
        verify(employeeRepository, never()).findById(employee.getId());
    }

    @Test
    void canRead_returnsFalse_whenPrincipalIsNotCustomUserDetails() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        assertThat(authorizationService.canRead(UUID.randomUUID(), authentication)).isFalse();
    }

    @Test
    void canRead_returnsFalse_whenEmployeeIdDoesNotExist() {
        UUID missingEmployeeId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(UUID.randomUUID());

        when(employeeRepository.findById(missingEmployeeId)).thenReturn(Optional.empty());

        assertThat(authorizationService.canRead(missingEmployeeId, authentication)).isFalse();
    }

    @Test
    void canManage_returnsFalse_whenAuthenticationIsNull() {
        assertThat(authorizationService.canManage(null)).isFalse();
    }

    @Test
    void canManage_returnsFalse_whenAuthenticationIsNotAuthenticated() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("jane.doe", "credentials");

        assertThat(authorizationService.canManage(authentication)).isFalse();
    }

    @Test
    void canManage_returnsFalse_whenUserHasNoQualifyingRole() {
        Authentication authentication = authenticatedAs(UUID.randomUUID(), "ROLE_EMPLOYEE");

        assertThat(authorizationService.canManage(authentication)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_HR_OFFICER"})
    void canManage_returnsTrue_forEachQualifyingRole(String role) {
        Authentication authentication = authenticatedAs(UUID.randomUUID(), role);

        assertThat(authorizationService.canManage(authentication)).isTrue();
    }

    private static void setId(Employee employee, UUID id) {
        setField(employee, Employee.class, "id", id);
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
