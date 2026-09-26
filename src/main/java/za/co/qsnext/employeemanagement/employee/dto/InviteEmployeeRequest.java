package za.co.qsnext.employeemanagement.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Everything HR needs to onboard an existing staff member in one step: the
 * login is created for them and they set their own password from the
 * invitation email.
 */
public record InviteEmployeeRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username must not exceed 100 characters")
        String username,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @NotBlank(message = "Employee number is required")
        @Size(max = 50, message = "Employee number must not exceed 50 characters")
        String employeeNumber,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phoneNumber,

        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String jobTitle,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate
) {
}
