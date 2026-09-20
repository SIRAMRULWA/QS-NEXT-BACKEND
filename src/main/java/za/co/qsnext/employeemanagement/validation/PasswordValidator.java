package za.co.qsnext.employeemanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Enforces the platform's password policy: length is validated separately
 * via {@code @Size} on the DTO field, this only checks character variety.
 */
public class PasswordValidator
        implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHARACTER =
            Pattern.compile("[^A-Za-z0-9]");

    @Override
    public boolean isValid(
            String password,
            ConstraintValidatorContext context
    ) {

        if (password == null || password.isBlank()) {
            /*
             * Blank/missing passwords are rejected by @NotBlank.
             * Nothing further to check here.
             */
            return true;
        }

        return UPPERCASE.matcher(password).find()
                && LOWERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL_CHARACTER.matcher(password).find();
    }
}
