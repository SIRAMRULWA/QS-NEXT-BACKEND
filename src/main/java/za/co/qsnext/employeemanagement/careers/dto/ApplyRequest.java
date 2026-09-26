package za.co.qsnext.employeemanagement.careers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The applicant's name and phone, used the first time they apply to create
 * their candidate record.
 */
public record ApplyRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone
) {
}
