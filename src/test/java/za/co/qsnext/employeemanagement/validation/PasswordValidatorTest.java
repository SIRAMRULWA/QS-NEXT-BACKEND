package za.co.qsnext.employeemanagement.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();
    private final ConstraintValidatorContext context =
            mock(ConstraintValidatorContext.class);

    @Test
    void nullIsValid_presenceIsEnforcedSeparately() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void emptyIsValid_presenceIsEnforcedSeparately() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password1",
            "Str0ngPass",
            "aB3defgh",
            "1234567Xa"
    })
    void passwordsWithUpperLowerAndDigitAreValid(String password) {
        assertThat(validator.isValid(password, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "alllowercase1",
            "ALLUPPERCASE1",
            "NoDigitsHere",
            "12345678"
    })
    void passwordsMissingAComponentAreInvalid(String password) {
        assertThat(validator.isValid(password, context)).isFalse();
    }
}
