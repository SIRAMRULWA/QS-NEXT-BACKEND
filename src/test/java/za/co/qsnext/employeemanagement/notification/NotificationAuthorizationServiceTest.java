package za.co.qsnext.employeemanagement.notification;

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

import za.co.qsnext.employeemanagement.security.CachedUserPrincipal;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAuthorizationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new NotificationAuthorizationService(notificationRepository);
    }

    private Authentication authenticatedAs(UUID userId, String... roles) {
        CustomUserDetails principal =
                new CustomUserDetails(
                        new CachedUserPrincipal(userId, "jane.doe", true, true, Set.of()));

        List<SimpleGrantedAuthority> authorities =
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Notification notificationFor(UUID userId) {
        Notification notification =
                new Notification(userId, "LEAVE_APPROVED", "Leave approved", "Your leave was approved");
        setId(notification, UUID.randomUUID());
        return notification;
    }

    @Test
    void canAccessUserNotifications_returnsFalse_whenAuthenticationIsNull() {
        assertThat(authorizationService.canAccessUserNotifications(UUID.randomUUID(), null))
                .isFalse();
    }

    @Test
    void canAccessUserNotifications_returnsFalse_whenAuthenticationIsNotAuthenticated() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("jane.doe", "credentials");

        assertThat(
                authorizationService.canAccessUserNotifications(
                        UUID.randomUUID(), authentication))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_HR_OFFICER"})
    void canAccessUserNotifications_returnsTrue_forEachQualifyingRole_evenForAnotherUser(
            String role) {
        UUID targetUserId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(UUID.randomUUID(), role);

        assertThat(
                authorizationService.canAccessUserNotifications(targetUserId, authentication))
                .isTrue();
    }

    @Test
    void canAccessUserNotifications_returnsTrue_whenUserIsRequestingOwnNotifications() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(userId);

        assertThat(authorizationService.canAccessUserNotifications(userId, authentication))
                .isTrue();
    }

    @Test
    void canAccessUserNotifications_returnsFalse_whenUserRequestsAnotherUsersNotifications() {
        Authentication authentication = authenticatedAs(UUID.randomUUID());

        assertThat(
                authorizationService.canAccessUserNotifications(
                        UUID.randomUUID(), authentication))
                .isFalse();
    }

    @Test
    void canAccessUserNotifications_returnsFalse_whenPrincipalIsNotCustomUserDetails() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        assertThat(
                authorizationService.canAccessUserNotifications(
                        UUID.randomUUID(), authentication))
                .isFalse();
    }

    @Test
    void canAccessNotification_returnsFalse_whenAuthenticationIsNull() {
        assertThat(authorizationService.canAccessNotification(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    void canAccessNotification_returnsFalse_whenAuthenticationIsNotAuthenticated() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("jane.doe", "credentials");

        assertThat(
                authorizationService.canAccessNotification(UUID.randomUUID(), authentication))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_HR_OFFICER"})
    void canAccessNotification_returnsTrue_forEachQualifyingRole_withoutQueryingRepository(
            String role) {
        UUID notificationId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(UUID.randomUUID(), role);

        assertThat(authorizationService.canAccessNotification(notificationId, authentication))
                .isTrue();
        verify(notificationRepository, never()).findById(notificationId);
    }

    @Test
    void canAccessNotification_returnsTrue_whenNotificationBelongsToCurrentUser() {
        UUID userId = UUID.randomUUID();
        Notification notification = notificationFor(userId);
        Authentication authentication = authenticatedAs(userId);

        when(notificationRepository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        assertThat(
                authorizationService.canAccessNotification(notification.getId(), authentication))
                .isTrue();
    }

    @Test
    void canAccessNotification_returnsFalse_whenNotificationBelongsToAnotherUser() {
        Notification notification = notificationFor(UUID.randomUUID());
        Authentication authentication = authenticatedAs(UUID.randomUUID());

        when(notificationRepository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        assertThat(
                authorizationService.canAccessNotification(notification.getId(), authentication))
                .isFalse();
    }

    @Test
    void canAccessNotification_returnsFalse_whenNotificationDoesNotExist() {
        UUID missingNotificationId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(UUID.randomUUID());

        when(notificationRepository.findById(missingNotificationId)).thenReturn(Optional.empty());

        assertThat(
                authorizationService.canAccessNotification(
                        missingNotificationId, authentication))
                .isFalse();
    }

    @Test
    void canAccessNotification_returnsFalse_whenPrincipalIsNotCustomUserDetails() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        assertThat(
                authorizationService.canAccessNotification(UUID.randomUUID(), authentication))
                .isFalse();
    }

    private static void setId(Notification notification, UUID id) {
        setField(notification, Notification.class, "id", id);
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
