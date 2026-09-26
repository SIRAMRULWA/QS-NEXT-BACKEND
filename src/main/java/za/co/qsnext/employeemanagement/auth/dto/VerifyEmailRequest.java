package za.co.qsnext.employeemanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(

        @NotBlank(message = "Verification code is required")
        @Size(max = 200, message = "Verification code is too long")
        String token
) {
}
