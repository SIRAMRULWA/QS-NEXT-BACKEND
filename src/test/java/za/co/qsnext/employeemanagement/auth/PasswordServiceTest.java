package za.co.qsnext.employeemanagement.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(passwordEncoder);
    }

    @Test
    void encode_delegatesToThePasswordEncoder() {
        when(passwordEncoder.encode("S3curePass!")).thenReturn("hashed-value");

        String result = passwordService.encode("S3curePass!");

        assertThat(result).isEqualTo("hashed-value");
        verify(passwordEncoder).encode("S3curePass!");
    }

    @Test
    void matches_returnsTrue_whenRawPasswordMatchesTheHash() {
        when(passwordEncoder.matches("S3curePass!", "hashed-value")).thenReturn(true);

        boolean result = passwordService.matches("S3curePass!", "hashed-value");

        assertThat(result).isTrue();
    }

    @Test
    void matches_returnsFalse_whenRawPasswordDoesNotMatchTheHash() {
        when(passwordEncoder.matches("wrong-password", "hashed-value")).thenReturn(false);

        boolean result = passwordService.matches("wrong-password", "hashed-value");

        assertThat(result).isFalse();
    }
}
