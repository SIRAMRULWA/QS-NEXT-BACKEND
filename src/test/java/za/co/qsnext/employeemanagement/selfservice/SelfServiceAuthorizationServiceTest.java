package za.co.qsnext.employeemanagement.selfservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import za.co.qsnext.employeemanagement.security.CachedUserPrincipal;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SelfServiceAuthorizationServiceTest {

    private SelfServiceAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new SelfServiceAuthorizationService();
    }

    private Authentication authenticatedAs(UUID userId) {
        CustomUserDetails principal =
                new CustomUserDetails(
                        new CachedUserPrincipal(userId, "jane.doe", true, true, Set.of()));

        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void canAccessOwnProfile_returnsFalse_whenAuthenticationIsNull() {
        assertThat(authorizationService.canAccessOwnProfile(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    void canAccessOwnProfile_returnsFalse_whenAuthenticationIsNotAuthenticated() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("jane.doe", "credentials");

        assertThat(authorizationService.canAccessOwnProfile(UUID.randomUUID(), authentication))
                .isFalse();
    }

    @Test
    void canAccessOwnProfile_returnsFalse_whenPrincipalIsNotCustomUserDetails() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        assertThat(authorizationService.canAccessOwnProfile(UUID.randomUUID(), authentication))
                .isFalse();
    }

    @Test
    void canAccessOwnProfile_returnsTrue_whenUserIdMatchesAuthenticatedPrincipal() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = authenticatedAs(userId);

        assertThat(authorizationService.canAccessOwnProfile(userId, authentication)).isTrue();
    }

    @Test
    void canAccessOwnProfile_returnsFalse_whenUserIdDoesNotMatchAuthenticatedPrincipal() {
        Authentication authentication = authenticatedAs(UUID.randomUUID());

        assertThat(authorizationService.canAccessOwnProfile(UUID.randomUUID(), authentication))
                .isFalse();
    }
}
