package za.co.qsnext.employeemanagement.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.auth.dto.ChangePasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.ForgotPasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginResponse;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.ResetPasswordRequest;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.AccountLockedException;
import za.co.qsnext.employeemanagement.exception.UnauthorizedException;
import za.co.qsnext.employeemanagement.security.ClientIpResolver;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.security.JwtService;
import za.co.qsnext.employeemanagement.security.TokenRevocationService;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;
import za.co.qsnext.employeemanagement.user.UserService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the authentication hardening added in Phase 1: failed
 * login lockout, refresh token rotation and reuse detection, password
 * change/reset. The repository/service collaborators are mocked; only
 * {@link JwtService} is real, since its token generation/parsing has no
 * external dependencies and using the real implementation makes the
 * rotation/reuse tests exercise genuine signed tokens.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET =
            "unit-test-jwt-signing-secret-do-not-use-in-production-0123456789";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditService auditService;
    @Mock
    private TokenRevocationService tokenRevocationService;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);

        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordService,
                jwtService,
                authenticationManager,
                userService,
                refreshTokenService,
                refreshTokenRepository,
                passwordResetTokenRepository,
                emailService,
                auditService,
                tokenRevocationService,
                new ClientIpResolver(""),
                MAX_FAILED_LOGIN_ATTEMPTS,
                15,
                30
        );
    }

    private User userWithId() {
        User user = new User("jane.doe", "jane.doe@qsnext.co.za", "hashed-password");
        setId(user, UUID.randomUUID());
        return user;
    }

    private static void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void login_returnsTokens_whenCredentialsAreValid() {
        User user = userWithId();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(refreshTokenService.issue(eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn("issued-refresh-token");

        LoginResponse response = authService.login(
                new LoginRequest("jane.doe", "S3curePass!"), null);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isEqualTo("issued-refresh-token");
        assertThat(response.username()).isEqualTo("jane.doe");

        verify(userService).recordSuccessfulLogin(user.getId());
        verify(auditService).log(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), AuditService.RESULT_SUCCESS);
    }

    @Test
    void login_registersFailedAttempt_onBadCredentials() {
        User user = userWithId();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("jane.doe", "wrong-password"), null))
                .isInstanceOf(UnauthorizedException.class);

        verify(userService).registerFailedLoginAttempt(
                eq(user.getId()), eq(MAX_FAILED_LOGIN_ATTEMPTS), eq(Duration.ofMinutes(15)));
        verify(auditService).log(user.getId(), "LOGIN_FAILURE", "USER", user.getId(), AuditService.RESULT_FAILURE);
    }

    @Test
    void login_doesNotRegisterFailedAttempt_forUnknownUsername() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("ghost", "whatever"), null))
                .isInstanceOf(UnauthorizedException.class);

        verify(userService, never()).registerFailedLoginAttempt(any(), anyInt(), any());
    }

    @Test
    void login_throwsAccountLocked_whenAccountIsLocked() {
        User user = userWithId();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("Account is locked"));
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("jane.doe", "S3curePass!"), null))
                .isInstanceOf(AccountLockedException.class);

        verify(userService, never()).registerFailedLoginAttempt(any(), anyInt(), any());
        verify(auditService).log(user.getId(), "LOGIN_FAILURE", "USER", user.getId(), AuditService.RESULT_FAILURE);
    }

    @Test
    void refreshToken_rotatesTheToken_whenItIsActive() {
        User user = userWithId();

        JwtService.IssuedRefreshToken issued =
                jwtService.generateRefreshToken(user.getId(), user.getUsername());

        RefreshToken storedToken = new RefreshToken(
                issued.tokenId(), user.getId(), issued.expiresAt(), "127.0.0.1");

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findById(issued.tokenId())).thenReturn(Optional.of(storedToken));
        when(refreshTokenService.issue(eq(user.getId()), eq(user.getUsername()), any()))
                .thenReturn("new-refresh-token");

        LoginResponse response = authService.refreshToken(
                new RefreshTokenRequest(issued.token()), null);

        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(storedToken.isRevoked()).isTrue();
        verify(auditService).log(user.getId(), "TOKEN_REFRESH", "USER", user.getId(), AuditService.RESULT_SUCCESS);
        verify(refreshTokenService, never()).revokeAllActiveForUser(any());
    }

    @Test
    void refreshToken_revokesAllTokens_whenAnAlreadyRotatedTokenIsReplayed() {
        User user = userWithId();

        JwtService.IssuedRefreshToken issued =
                jwtService.generateRefreshToken(user.getId(), user.getUsername());

        RefreshToken storedToken = new RefreshToken(
                issued.tokenId(), user.getId(), issued.expiresAt(), "127.0.0.1");
        storedToken.revoke();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findById(issued.tokenId())).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() ->
                authService.refreshToken(new RefreshTokenRequest(issued.token()), null))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenService).revokeAllActiveForUser(user.getId());
        verify(auditService).log(
                user.getId(), "TOKEN_REUSE_DETECTED", "USER", user.getId(), AuditService.RESULT_FAILURE);
        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void logout_revokesBothTheAccessTokenAndTheRefreshToken() {
        User user = userWithId();

        JwtService.IssuedRefreshToken issued =
                jwtService.generateRefreshToken(user.getId(), user.getUsername());

        RefreshToken storedToken = new RefreshToken(
                issued.tokenId(), user.getId(), issued.expiresAt(), "127.0.0.1");

        when(refreshTokenService.find(issued.tokenId())).thenReturn(Optional.of(storedToken));

        authService.logout(new RefreshTokenRequest(issued.token()), "access-token");

        verify(tokenRevocationService).revoke("access-token");
        verify(refreshTokenService).revoke(storedToken);
        verify(auditService).log(
                user.getId(), "LOGOUT", "USER", user.getId(), AuditService.RESULT_SUCCESS);
    }

    @Test
    void logout_revokesOnlyTheAccessToken_whenNoRefreshTokenIsGiven() {
        authService.logout(new RefreshTokenRequest(""), "access-token");

        verify(tokenRevocationService).revoke("access-token");
        verify(refreshTokenService, never()).find(any());
    }

    @Test
    void changePassword_updatesPassword_whenCurrentPasswordMatches() {
        User user = userWithId();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordService.matches("old-pass", user.getPasswordHash())).thenReturn(true);
        when(passwordService.encode("N3wPass!word")).thenReturn("new-hash");

        authService.changePassword(
                user.getId(), new ChangePasswordRequest("old-pass", "N3wPass!word"), "access-token");

        verify(userService).changePassword(user.getId(), "new-hash");
        verify(refreshTokenService).revokeAllActiveForUser(user.getId());
        verify(tokenRevocationService).revoke("access-token");
        verify(auditService).log(user.getId(), "PASSWORD_CHANGE", "USER", user.getId(), AuditService.RESULT_SUCCESS);
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        User user = userWithId();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordService.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword(
                        user.getId(), new ChangePasswordRequest("wrong", "N3wPass!word"), "access-token"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userService, never()).changePassword(any(), any());
        verify(refreshTokenService, never()).revokeAllActiveForUser(any());
    }

    @Test
    void forgotPassword_createsResetTokenAndEmail_whenUserExists() {
        User user = userWithId();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).queueEmail(eq(EmailTemplate.PASSWORD_RESET), eq(user.getEmail()), any());
        verify(auditService).log(
                user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(), AuditService.RESULT_SUCCESS);
    }

    @Test
    void forgotPassword_doesNothing_whenEmailIsUnknown() {
        when(userRepository.findByEmail("nobody@qsnext.co.za")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("nobody@qsnext.co.za"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).queueEmail(any(), any(), any());
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("bogus-token", "N3wPass!word")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resetPassword_rejectsAlreadyUsedToken() {
        PasswordResetToken usedToken = new PasswordResetToken(
                UUID.randomUUID(), "hash", OffsetDateTime.now().plusMinutes(30));
        usedToken.markUsed();

        when(passwordResetTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("some-token", "N3wPass!word")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
