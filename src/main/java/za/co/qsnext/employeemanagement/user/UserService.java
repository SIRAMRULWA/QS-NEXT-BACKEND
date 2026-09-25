package za.co.qsnext.employeemanagement.user;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.config.RedisCacheNames;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final EmailService emailService;

    public UserService(
            UserRepository userRepository,
            CacheManager cacheManager,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.emailService = emailService;
    }

    public Page<User> search(String query, Pageable pageable) {
        return userRepository.search(query, pageable);
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
        evictAuthorizationCache(user.getUsername());
    }

    @Transactional
    public void enable(UUID userId) {
        User user = getById(userId);
        user.enable();
        evictAuthorizationCache(user.getUsername());
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

            OffsetDateTime lockedUntil = OffsetDateTime.now().plus(accountLockDuration);
            user.lockUntil(lockedUntil);
            evictAuthorizationCache(user.getUsername());

            emailService.queueEmail(
                    EmailTemplate.ACCOUNT_LOCKED,
                    user.getEmail(),
                    Map.of("lockedUntil", lockedUntil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
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
        evictAuthorizationCache(user.getUsername());
    }

    /**
     * Evicts the cached authorization principal (used by
     * {@code JwtAuthenticationFilter} on every request) so an enabled,
     * disabled or lock state change takes effect immediately rather than
     * waiting out the cache TTL.
     */
    private void evictAuthorizationCache(String username) {
        Cache cache = cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS);

        if (cache != null) {
            cache.evict(username);
        }
    }

    @Transactional
    public void changePassword(UUID userId, String newPasswordHash) {
        User user = getById(userId);
        user.changePassword(newPasswordHash);
    }
}