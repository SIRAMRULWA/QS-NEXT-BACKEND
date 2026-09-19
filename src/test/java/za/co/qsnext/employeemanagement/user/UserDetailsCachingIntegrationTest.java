package za.co.qsnext.employeemanagement.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;
import za.co.qsnext.employeemanagement.security.CustomUserDetailsService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the userDetails cache against real Redis: this cache backs
 * authorization on every authenticated request, so it must never keep
 * serving a disabled user as enabled once UserService has evicted it -
 * that would be exactly the "stale authorization data" the caching
 * design is required to avoid.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserDetailsCachingIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void disablingThroughTheServiceImmediatelyEvictsTheCachedPrincipal() {

        String username = "cache_user_" + UUID.randomUUID().toString().substring(0, 8);

        User user = userService.create(
                username,
                username + "@example.com",
                "irrelevant-hash-for-this-test"
        );

        UserDetails firstLookup =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(firstLookup.isEnabled()).isTrue();

        /*
         * Disable the underlying row directly through the repository,
         * bypassing UserService.disable() (and therefore its cache
         * eviction), to prove the second lookup below is actually
         * served from cache rather than coincidentally hitting the DB.
         */
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.disable();
        userRepository.save(managed);

        UserDetails stillCachedAsEnabled =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(stillCachedAsEnabled.isEnabled()).isTrue();

        /*
         * Going through the real service method evicts the cache, so
         * the disabled state must be visible on the very next lookup -
         * not after the TTL expires.
         */
        userService.disable(user.getId());

        UserDetails afterEviction =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(afterEviction.isEnabled()).isFalse();
    }

    @Test
    void changingPasswordEvictsTheCachedPrincipal() {

        String username = "cache_user_" + UUID.randomUUID().toString().substring(0, 8);
        String originalHash = "$2a$originalHash";

        User user = userService.create(
                username,
                username + "@example.com",
                originalHash
        );

        UserDetails firstLookup =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(firstLookup.getPassword()).isEqualTo(originalHash);

        /*
         * Change the password hash directly, bypassing
         * UserService.changePassword()'s eviction, to prove the cache
         * is genuinely serving the earlier lookup rather than
         * recomputing it.
         */
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.changePassword("$2a$changedDirectly");
        userRepository.save(managed);

        UserDetails stillCachedWithOldHash =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(stillCachedWithOldHash.getPassword()).isEqualTo(originalHash);

        customUserDetailsService.evictUser(username);

        UserDetails afterEviction =
                customUserDetailsService.loadUserByUsername(username);
        assertThat(afterEviction.getPassword()).isEqualTo("$2a$changedDirectly");
    }
}
