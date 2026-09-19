package za.co.qsnext.employeemanagement.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
     * Records a failed login attempt and locks the account once the
     * configured threshold is reached. Runs in its own transaction so the
     * attempt is persisted even though the caller's login transaction is
     * about to roll back after throwing an authentication failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedLoginAttempt(
            UUID userId,
            int maxFailedLoginAttempts,
            Duration accountLockDuration
    ) {
        User user = getById(userId);

        int attempts = user.incrementFailedLoginAttempts();

        if (attempts >= maxFailedLoginAttempts) {
            user.lockUntil(
                    OffsetDateTime.now().plus(accountLockDuration)
            );
        }
    }

    /**
     * Resets the failed-login counter and records the successful login
     * time. Runs in its own transaction so it is not affected by whatever
     * the caller does afterwards in the same login flow.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(UUID userId) {
        User user = getById(userId);
        user.recordSuccessfulLogin();
    }

    @Transactional
    public void changePassword(UUID userId, String newPasswordHash) {
        User user = getById(userId);
        user.changePassword(newPasswordHash);
    }
}