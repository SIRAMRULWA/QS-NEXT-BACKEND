package za.co.qsnext.employeemanagement.user;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserLockoutTest {

    @Test
    void incrementFailedLoginAttempts_countsUpFromZero() {
        User user = new User("jane", "jane@qsnext.co.za", "hash");

        assertThat(user.incrementFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.incrementFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    void isLocked_isFalse_whenLockedUntilIsNotSet() {
        User user = new User("jane", "jane@qsnext.co.za", "hash");

        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void isLocked_isTrue_whileLockedUntilIsInTheFuture() {
        User user = new User("jane", "jane@qsnext.co.za", "hash");

        user.lockUntil(OffsetDateTime.now().plusMinutes(15));

        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void isLocked_isFalse_onceLockedUntilHasPassed() {
        User user = new User("jane", "jane@qsnext.co.za", "hash");

        user.lockUntil(OffsetDateTime.now().minusSeconds(1));

        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void recordSuccessfulLogin_resetsFailedAttemptsAndUnlocks() {
        User user = new User("jane", "jane@qsnext.co.za", "hash");

        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();
        user.lockUntil(OffsetDateTime.now().plusMinutes(15));

        user.recordSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getLastLoginAt()).isNotNull();
    }
}
