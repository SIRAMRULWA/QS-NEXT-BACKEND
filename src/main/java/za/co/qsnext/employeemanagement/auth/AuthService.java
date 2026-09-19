package za.co.qsnext.employeemanagement.auth;

import io.jsonwebtoken.JwtException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginResponse;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UnauthorizedException;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;
import za.co.qsnext.employeemanagement.security.JwtService;
import za.co.qsnext.employeemanagement.security.RefreshTokenService;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordService passwordService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponse login(LoginRequest request) {

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.username(),
                                    request.password()
                            )
                    );

            Object principal = authentication.getPrincipal();

            if (!(principal instanceof CustomUserDetails userDetails)) {
                throw new UnauthorizedException(
                        "Authentication failed"
                );
            }

            if (!userDetails.isEnabled()) {
                throw new UnauthorizedException(
                        "User account is disabled"
                );
            }

            return createLoginResponse(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getEmail()
            );

        } catch (AuthenticationException ex) {

            throw new UnauthorizedException(
                    "Invalid username or password"
            );
        }
    }

    @Transactional
    public LoginResponse register(
            RegisterRequest request
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

        return createLoginResponse(savedUser);
    }

    public LoginResponse refreshToken(
            RefreshTokenRequest request
    ) {

        if (request.refreshToken() == null
                || request.refreshToken().isBlank()) {

            throw new UnauthorizedException(
                    "Refresh token is required"
            );
        }

        String refreshToken =
                request.refreshToken();

        try {

            if (!jwtService.isRefreshToken(refreshToken)) {

                throw new UnauthorizedException(
                        "Invalid refresh token"
                );
            }

            String username =
                    jwtService.extractUsername(
                            refreshToken
                    );

            User user =
                    userRepository
                            .findByUsername(username)
                            .orElseThrow(() ->
                                    new UnauthorizedException(
                                            "Invalid refresh token"
                                    )
                            );

            if (!user.isEnabled()) {

                throw new UnauthorizedException(
                        "User account is disabled"
                );
            }

            if (!jwtService.isTokenValid(
                    refreshToken,
                    user.getUsername()
            )) {

                throw new UnauthorizedException(
                        "Invalid or expired refresh token"
                );
            }

            String tokenId =
                    jwtService.extractTokenId(refreshToken);

            if (tokenId == null
                    || !refreshTokenService.isActive(
                    user.getId(),
                    tokenId
            )) {

                /*
                 * Token is well-formed and unexpired but has already
                 * been used (rotation is single-use) or was revoked
                 * via logout/password change.
                 */
                throw new UnauthorizedException(
                        "Refresh token has been revoked or already used"
                );
            }

            /*
             * Rotate: the presented refresh token is single-use. Revoke
             * it before issuing the replacement pair so a stolen token
             * cannot be replayed after the legitimate client rotates it.
             */
            refreshTokenService.revoke(
                    user.getId(),
                    tokenId
            );

            return createLoginResponse(user);

        } catch (JwtException
                 | IllegalArgumentException ex) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }
    }

    /**
     * Revokes the presented refresh token so it can no longer be used to
     * mint new access tokens. Idempotent and deliberately silent about
     * whether the token was valid, already expired, or already revoked,
     * so it does not leak session state to a caller.
     */
    @Transactional
    public void logout(
            RefreshTokenRequest request
    ) {

        String refreshToken = request.refreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {

            if (!jwtService.isRefreshToken(refreshToken)) {
                return;
            }

            UUID userId =
                    jwtService.extractUserId(refreshToken);

            String tokenId =
                    jwtService.extractTokenId(refreshToken);

            if (tokenId != null) {
                refreshTokenService.revoke(userId, tokenId);
            }

        } catch (JwtException | IllegalArgumentException ex) {

            /*
             * Malformed, expired or otherwise invalid token: nothing to
             * revoke. Logout still succeeds from the caller's point of
             * view.
             */
        }
    }

    private LoginResponse createLoginResponse(
            User user
    ) {
        return createLoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    private LoginResponse createLoginResponse(
            UUID userId,
            String username,
            String email
    ) {

        String accessToken =
                jwtService.generateAccessToken(
                        userId,
                        username
                );

        String tokenId =
                UUID.randomUUID().toString();

        String refreshToken =
                jwtService.generateRefreshToken(
                        userId,
                        username,
                        tokenId
                );

        refreshTokenService.store(
                userId,
                tokenId,
                Duration.ofMillis(
                        jwtService.getRefreshTokenExpiration()
                )
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtService.getAccessTokenExpiration(),
                userId,
                username,
                email
        );
    }
}