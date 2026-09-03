package za.co.qsnext.employeemanagement.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginResponse;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UnauthorizedException;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid username or password"
                        )
                );

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    "User account is disabled"
            );
        }

        if (!passwordService.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException(
                    "Invalid username or password"
            );
        }

        /*
         * JWT access-token and refresh-token generation
         * will be connected when JwtService is implemented.
         */
        return LoginResponse.fromAuthenticatedUser(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                    "Username already exists: "
                            + request.username()
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Email already exists: "
                            + request.email()
            );
        }

        String passwordHash =
                passwordService.encode(request.password());

        User user = new User(
                request.username(),
                request.email(),
                passwordHash
        );

        User savedUser = userRepository.save(user);

        /*
         * JWT generation will be connected here once
         * JwtService is implemented.
         */
        return LoginResponse.fromAuthenticatedUser(savedUser);
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

        /*
         * Refresh-token validation and rotation will be
         * implemented together with JwtService.
         */
        throw new UnauthorizedException(
                "Refresh token authentication is not yet configured"
        );
    }
}