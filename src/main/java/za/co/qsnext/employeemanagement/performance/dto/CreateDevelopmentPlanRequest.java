package za.co.qsnext.employeemanagement.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDevelopmentPlanRequest(

        UUID reviewId,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID recommendedCourseId,

        LocalDate targetDate
) {
}
