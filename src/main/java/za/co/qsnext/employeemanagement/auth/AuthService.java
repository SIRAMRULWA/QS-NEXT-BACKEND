package za.co.qsnext.employeemanagement.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.auth.dto.ChangePasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.ForgotPasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginResponse;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.auth.dto.ResetPasswordRequest;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.AccountLockedException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UnauthorizedException;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.security.JwtService;
import za.co.qsnext.employeemanagement.security.TokenRevocationService;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;
import za.co.qsnext.employeemanagement.user.UserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DEFAULT_ROLE = "EMPLOYEE";
    private static final String ENTITY_TYPE_USER = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final TokenRevocationService tokenRevocationService;

    private final int maxFailedLoginAttempts;
    private final Duration accountLockDuration;
    private final Duration passwordResetTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordService passwordService,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserService userService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            AuditService auditService,
            TokenRevocationService tokenRevocationService,
            @Value("${security.auth.max-failed-login-attempts}") int maxFailedLoginAttempts,
            @Value("${security.auth.account-lock-duration-minutes}") long accountLockDurationMinutes,
            @Value("${security.auth.password-reset-token-expiration-minutes}") long passwordResetTokenExpirationMinutes
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.tokenRevocationService = tokenRevocationService;
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
        this.accountLockDuration = Duration.ofMinutes(accountLockDurationMinutes);
        this.passwordResetTokenExpiration = Duration.ofMinutes(passwordResetTokenExpirationMinutes);
    }

    @Transactional
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.username(),
                                    request.password()
                            )
                    );

            CustomUserDetails userDetails =
                    (CustomUserDetails) authentication.getPrincipal();

            User user = userDetails.getUser();

            userService.recordSuccessfulLogin(user.getId());

            auditService.log(
                    user.getId(),
                    "LOGIN_SUCCESS",
                    ENTITY_TYPE_USER,
                    user.getId(),
                    AuditService.RESULT_SUCCESS
            );

            return createLoginResponse(user, resolveClientIp(httpRequest));

        } catch (LockedException ex) {

            findUserForAudit(request.username())
                    .ifPresent(user -> auditService.log(
                            user.getId(),
                            "LOGIN_FAILURE",
                            ENTITY_TYPE_USER,
                            user.getId(),
                            AuditService.RESULT_FAILURE
                    ));

            throw new AccountLockedException(
                    "Account is temporarily locked due to repeated failed login attempts"
            );

        } catch (DisabledException ex) {

            findUserForAudit(request.username())
                    .ifPresent(user -> auditService.log(
                            user.getId(),
                            "LOGIN_FAILURE",
                            ENTITY_TYPE_USER,
                            user.getId(),
                            AuditService.RESULT_FAILURE
                    ));

            throw new UnauthorizedException("Invalid username or password");

        } catch (BadCredentialsException ex) {

            findUserForAudit(request.username())
                    .ifPresent(user -> {
                        userService.registerFailedLoginAttempt(
                                user.getId(),
                                maxFailedLoginAttempts,
                                accountLockDuration
                        );

                        auditService.log(
                                user.getId(),
                                "LOGIN_FAILURE",
                                ENTITY_TYPE_USER,
                                user.getId(),
                                AuditService.RESULT_FAILURE
                        );
                    });

            throw new UnauthorizedException("Invalid username or password");

        } catch (AuthenticationException ex) {

            throw new UnauthorizedException("Invalid username or password");
        }
    }

    @Transactional
    public LoginResponse register(
            RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        if (userRepository.existsByUsername(
                request.username()
        )) {

            throw new DuplicateResourceException(
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(
                request.email()
        )) {

            throw new DuplicateResourceException(
                    "Email already exists"
            );
        }

        Role employeeRole =
                roleRepository.findByName(DEFAULT_ROLE)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "EMPLOYEE role is not configured"
                                )
                        );

        String passwordHash =
                passwordService.encode(
                        request.password()
                );

        User user = new User(
                request.username(),
                request.email(),
                passwordHash
        );

        user.assignRole(employeeRole);

        User savedUser =
                userRepository.save(user);

        emailService.queueEmail(
                EmailTemplate.WELCOME,
                savedUser.getEmail(),
                Map.of("username", savedUser.getUsername())
        );

        auditService.log(
                savedUser.getId(),
                "REGISTER",
                ENTITY_TYPE_USER,
                savedUser.getId(),
                AuditService.RESULT_SUCCESS
        );

        return createLoginResponse(savedUser, resolveClientIp(httpRequest));
    }

    @Transactional
    public LoginResponse refreshToken(
            RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        String presentedToken = request.refreshToken();

        UUID tokenId;
        String username;

        try {

            if (!jwtService.isRefreshToken(presentedToken)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            tokenId = jwtService.extractTokenId(presentedToken);
            username = jwtService.extractUsername(presentedToken);

            if (!jwtService.isTokenValid(presentedToken, username)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

        } catch (JwtException | IllegalArgumentException ex) {

            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new UnauthorizedException("Invalid refresh token")
                        );

        RefreshToken storedToken =
                refreshTokenRepository.findById(tokenId)
                        .orElseThrow(() ->
                                new UnauthorizedException("Invalid refresh token")
                        );

        if (!storedToken.getUserId().equals(user.getId())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (storedToken.isRevoked()) {

            /*
             * A revoked (already-rotated) refresh token was presented
             * again. This can only happen if the token was stolen and
             * used out of order, so every active token for the user is
             * revoked to cut off the attacker's access.
             */
            refreshTokenService.revokeAllActiveForUser(user.getId());

            auditService.log(
                    user.getId(),
                    "TOKEN_REUSE_DETECTED",
                    ENTITY_TYPE_USER,
                    user.getId(),
                    AuditService.RESULT_FAILURE
            );

            throw new UnauthorizedException("Invalid refresh token");
        }

        if (storedToken.isExpired()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        storedToken.revoke();

        auditService.log(
                user.getId(),
                "TOKEN_REFRESH",
                ENTITY_TYPE_USER,
                user.getId(),
                AuditService.RESULT_SUCCESS
        );

        return createLoginResponse(user, resolveClientIp(httpRequest));
    }

    @Transactional
    public void logout(
            RefreshTokenRequest request,
            String accessToken
    ) {

        tokenRevocationService.revoke(accessToken);

        if (request.refreshToken() == null
                || request.refreshToken().isBlank()) {
            return;
        }

        try {

            UUID tokenId = jwtService.extractTokenId(request.refreshToken());

            refreshTokenService.find(tokenId)
                    .ifPresent(token -> {
                        refreshTokenService.revoke(token);

                        auditService.log(
                                token.getUserId(),
                                "LOGOUT",
                                ENTITY_TYPE_USER,
                                token.getUserId(),
                                AuditService.RESULT_SUCCESS
                        );
                    });

        } catch (JwtException | IllegalArgumentException ex) {
            /*
             * Malformed token: nothing to revoke. Logout is idempotent
             * from the caller's point of view either way.
             */
        }
    }

    @Transactional
    public void changePassword(
            UUID userId,
            ChangePasswordRequest request,
            String accessToken
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {

            auditService.log(
                    user.getId(),
                    "PASSWORD_CHANGE",
                    ENTITY_TYPE_USER,
                    user.getId(),
                    AuditService.RESULT_FAILURE
            );

            throw new UnauthorizedException("Current password is incorrect");
        }

        userService.changePassword(
                user.getId(),
                passwordService.encode(request.newPassword())
        );

        refreshTokenService.revokeAllActiveForUser(user.getId());
        tokenRevocationService.revoke(accessToken);

        auditService.log(
                user.getId(),
                "PASSWORD_CHANGE",
                ENTITY_TYPE_USER,
                user.getId(),
                AuditService.RESULT_SUCCESS
        );
    }

    /**
     * Always succeeds from the caller's perspective, whether or not the
     * email belongs to a registered user, so this endpoint cannot be used
     * to enumerate registered accounts.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        Optional<User> maybeUser =
                userRepository.findByEmail(request.email());

        if (maybeUser.isEmpty()) {
            return;
        }

        User user = maybeUser.get();

        String rawToken = generateSecureToken();

        passwordResetTokenRepository.save(
                new PasswordResetToken(
                        user.getId(),
                        hashToken(rawToken),
                        OffsetDateTime.now().plus(passwordResetTokenExpiration)
                )
        );

        emailService.queueEmail(
                EmailTemplate.PASSWORD_RESET,
                user.getEmail(),
                Map.of(
                        "token", rawToken,
                        "expiresInMinutes", String.valueOf(passwordResetTokenExpiration.toMinutes())
                )
        );

        auditService.log(
                user.getId(),
                "PASSWORD_RESET_REQUESTED",
                ENTITY_TYPE_USER,
                user.getId(),
                AuditService.RESULT_SUCCESS
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHash(hashToken(request.token()))
                        .filter(PasswordResetToken::isValid)
                        .orElseThrow(() ->
                                new UnauthorizedException("Invalid or expired reset token")
                        );

        resetToken.markUsed();

        userService.changePassword(
                resetToken.getUserId(),
                passwordService.encode(request.newPassword())
        );

        refreshTokenService.revokeAllActiveForUser(resetToken.getUserId());

        auditService.log(
                resetToken.getUserId(),
                "PASSWORD_RESET_COMPLETED",
                ENTITY_TYPE_USER,
                resetToken.getUserId(),
                AuditService.RESULT_SUCCESS
        );
    }

    private LoginResponse createLoginResponse(
            User user,
            String clientIp
    ) {

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getUsername()
                );

        String refreshToken =
                refreshTokenService.issue(
                        user.getId(),
                        user.getUsername(),
                        clientIp
                );

        return new LoginResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtService.getAccessTokenExpiration(),
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    private Optional<User> findUserForAudit(String username) {
        return userRepository.findByUsername(username);
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {

        if (httpRequest == null) {
            return null;
        }

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return httpRequest.getRemoteAddr();
    }

    private String generateSecureToken() {

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
