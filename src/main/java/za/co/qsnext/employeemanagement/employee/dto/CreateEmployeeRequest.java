package za.co.qsnext.employeemanagement.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

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