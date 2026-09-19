package za.co.qsnext.employeemanagement.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import za.co.qsnext.employeemanagement.config.RedisCacheNames;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache authorizationCache;
    @Mock
    private EmailService emailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, cacheManager, emailService);
    }

    private User userWithId(String username) {
        User user = new User(username, username + "@qsnext.co.za", "hash");
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }

    @Test
    void disable_evictsTheAuthorizationCacheEntry() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS)).thenReturn(authorizationCache);

        userService.disable(user.getId());

        assertThat(user.isEnabled()).isFalse();
        verify(authorizationCache).evict("jane.doe");
    }

    @Test
    void enable_evictsTheAuthorizationCacheEntry() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS)).thenReturn(authorizationCache);

        userService.enable(user.getId());

        verify(authorizationCache).evict("jane.doe");
    }

    @Test
    void registerFailedLoginAttempt_doesNotEvictCache_belowTheThreshold() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.registerFailedLoginAttempt(user.getId(), 5, Duration.ofMinutes(15));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isLocked()).isFalse();
        verify(cacheManager, never()).getCache(RedisCacheNames.USER_PRINCIPALS);
        verify(emailService, never()).queueEmail(any(), any(), any());
    }

    @Test
    void registerFailedLoginAttempt_locksAndEvictsCache_atTheThreshold() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS)).thenReturn(authorizationCache);

        for (int i = 0; i < 5; i++) {
            userService.registerFailedLoginAttempt(user.getId(), 5, Duration.ofMinutes(15));
        }

        assertThat(user.isLocked()).isTrue();
        verify(authorizationCache).evict("jane.doe");
        verify(emailService).queueEmail(eq(EmailTemplate.ACCOUNT_LOCKED), eq(user.getEmail()), any());
    }

    @Test
    void recordSuccessfulLogin_evictsTheAuthorizationCacheEntry() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS)).thenReturn(authorizationCache);

        userService.recordSuccessfulLogin(user.getId());

        assertThat(user.getLastLoginAt()).isNotNull();
        verify(authorizationCache).evict("jane.doe");
    }

    @Test
    void evictAuthorizationCache_toleratesAMissingCache() {
        User user = userWithId("jane.doe");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cacheManager.getCache(RedisCacheNames.USER_PRINCIPALS)).thenReturn(null);

        // Should not throw even though the cache bean returns null.
        userService.enable(user.getId());
    }
}
