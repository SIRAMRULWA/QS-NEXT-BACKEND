package za.co.qsnext.employeemanagement.security;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Phase 2 changes to {@link JwtAuthenticationFilter}:
 * authorizing from the Redis-backed {@link UserAuthorizationCacheService}
 * instead of a fresh database load on every request, honoring individually
 * revoked access tokens, and rejecting a token whose user has since been
 * disabled or locked even though the token itself is still validly signed.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "unit-test-jwt-signing-secret-do-not-use-in-production-0123456789";

    @Mock
    private UserAuthorizationCacheService userAuthorizationCacheService;
    @Mock
    private TokenRevocationService tokenRevocationService;
    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        filter = new JwtAuthenticationFilter(
                jwtService, userAuthorizationCacheService, tokenRevocationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CachedUserPrincipal activePrincipal(UUID userId, String username) {
        return new CachedUserPrincipal(userId, username, true, true, Set.of("EMPLOYEE_READ"));
    }

    @Test
    void authenticates_whenTokenIsValidAndUserIsActive() throws Exception {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, "jane.doe");

        when(userAuthorizationCacheService.findPrincipal("jane.doe"))
                .thenReturn(Optional.of(activePrincipal(userId, "jane.doe")));
        when(tokenRevocationService.isRevoked(any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("jane.doe");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticate_whenTokenIsRevoked() throws Exception {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, "jane.doe");

        when(tokenRevocationService.isRevoked(any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userAuthorizationCacheService, org.mockito.Mockito.never()).findPrincipal(any());
    }

    @Test
    void doesNotAuthenticate_whenUserHasSinceBeenDisabled() throws Exception {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, "jane.doe");

        CachedUserPrincipal disabledPrincipal =
                new CachedUserPrincipal(userId, "jane.doe", false, true, Set.of("EMPLOYEE_READ"));

        when(userAuthorizationCacheService.findPrincipal("jane.doe"))
                .thenReturn(Optional.of(disabledPrincipal));
        when(tokenRevocationService.isRevoked(any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticate_whenUserHasSinceBeenLocked() throws Exception {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, "jane.doe");

        CachedUserPrincipal lockedPrincipal =
                new CachedUserPrincipal(userId, "jane.doe", true, false, Set.of("EMPLOYEE_READ"));

        when(userAuthorizationCacheService.findPrincipal("jane.doe"))
                .thenReturn(Optional.of(lockedPrincipal));
        when(tokenRevocationService.isRevoked(any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticate_whenNoAuthorizationHeaderIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userAuthorizationCacheService, org.mockito.Mockito.never()).findPrincipal(any());
    }

    @Test
    void doesNotAuthenticate_whenTokenIsARefreshTokenNotAnAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtService.IssuedRefreshToken refreshToken =
                jwtService.generateRefreshToken(userId, "jane.doe");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken.token());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userAuthorizationCacheService, org.mockito.Mockito.never()).findPrincipal(any());
    }
}
