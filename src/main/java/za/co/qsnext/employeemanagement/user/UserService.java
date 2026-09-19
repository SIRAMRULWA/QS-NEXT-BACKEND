package za.co.qsnext.employeemanagement.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.auth.PasswordService;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UnauthorizedException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;
import za.co.qsnext.employeemanagement.security.RefreshTokenService;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            PasswordService passwordService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.refreshTokenService = refreshTokenService;
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found: " + userId
                        )
                );
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found: " + username
                        )
                );
    }

    @Transactional
    public User create(
            String username,
            String email,
            String passwordHash
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException(
                    "Username already exists: " + username
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email already exists: " + email
            );
        }

        User user = new User(
                username,
                email,
                passwordHash
        );

        return userRepository.save(user);
    }

    @Transactional
    public void disable(UUID userId) {
        User user = getById(userId);
        user.disable();
    }

    @Transactional
    public void enable(UUID userId) {
        User user = getById(userId);
        user.enable();
    }

    /**
     * Self-service password change. Revokes every refresh token issued
     * to the user so sessions established under the old password stop
     * working once it is no longer trustable.
     */
    @Transactional
    public void changePassword(
            UUID userId,
            String currentPassword,
            String newPassword
    ) {
        User user = getById(userId);

        if (!passwordService.matches(
                currentPassword,
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException(
                    "Current password is incorrect"
            );
        }

        user.changePassword(
                passwordService.encode(newPassword)
        );

        refreshTokenService.revokeAll(userId);
    }
}