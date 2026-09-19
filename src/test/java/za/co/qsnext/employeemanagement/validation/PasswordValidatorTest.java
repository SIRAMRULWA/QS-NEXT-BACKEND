package za.co.qsnext.employeemanagement.validation;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
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
    void isValid_acceptsPasswordWithAllRequiredCharacterClasses() {
        assertThat(validator.isValid("S3curePass!", context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "alllowercase1!",  // missing uppercase
            "ALLUPPERCASE1!",  // missing lowercase
            "NoDigitsHere!",   // missing digit
            "NoSpecial123",    // missing special character
    })
    void isValid_rejectsPasswordsMissingACharacterClass(String password) {
        assertThat(validator.isValid(password, context)).isFalse();
    }

    @Test
    void isValid_treatsBlankAsValid_leavingItToNotBlank() {
        assertThat(validator.isValid("", context)).isTrue();
        assertThat(validator.isValid(null, context)).isTrue();
    }
}
