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
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
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

            User user = userDetails.getUser();

            if (!user.isEnabled()) {
                throw new UnauthorizedException(
                        "User account is disabled"
                );
            }

            return createLoginResponse(user);

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

        String passwordHash =
                passwordService.encode(
                        request.password()
                );

        User user = new User(
                request.username(),
                request.email(),
                passwordHash
        );

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

            return createLoginResponse(user);

        } catch (JwtException
                 | IllegalArgumentException ex) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }
    }

    private LoginResponse createLoginResponse(
            User user
    ) {

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getUsername()
                );

        String refreshToken =
                jwtService.generateRefreshToken(
                        user.getId(),
                        user.getUsername()
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
}