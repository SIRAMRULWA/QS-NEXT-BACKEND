package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Converts an accepted {@code Offer}'s candidate into a real Employee -
 * the "integrate successful candidates with onboarding" step. A
 * {@code User} account is created for them with a random, unusable
 * password; they receive a password-reset email (see
 * {@code OfferService#hire}) to set their own, exactly like the
 * self-service "forgot password" flow.
 */
public record HireRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username must not exceed 100 characters")
        String username,

        @NotBlank(message = "Employee number is required")
        @Size(max = 50, message = "Employee number must not exceed 50 characters")
        String employeeNumber,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        /**
         * Optional - when provided, an onboarding workflow is started for
         * the new employee immediately (see the Onboarding module).
         */
        UUID onboardingTemplateId
) {
}
