package za.co.qsnext.employeemanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator
        implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(
            String password,
            ConstraintValidatorContext context
    ) {

        if (password == null || password.isEmpty()) {
            /*
             * Presence is enforced separately by @NotBlank; this
             * validator only checks composition.
             */
            return true;
        }

        boolean hasUppercase =
                password.chars().anyMatch(Character::isUpperCase);

        boolean hasLowercase =
                password.chars().anyMatch(Character::isLowerCase);

        boolean hasDigit =
                password.chars().anyMatch(Character::isDigit);

        return hasUppercase && hasLowercase && hasDigit;
    }
}
