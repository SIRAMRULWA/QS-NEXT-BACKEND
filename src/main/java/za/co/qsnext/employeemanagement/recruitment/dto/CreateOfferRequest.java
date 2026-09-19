package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOfferRequest(

        @NotNull(message = "Application ID is required")
        UUID applicationId,

        @NotBlank(message = "Job title is required")
        @Size(max = 200, message = "Job title must not exceed 200 characters")
        String jobTitle,

        @NotNull(message = "Salary amount is required")
        @DecimalMin(value = "0.01", message = "Salary amount must be greater than zero")
        BigDecimal salaryAmount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        LocalDate startDate
) {
}
