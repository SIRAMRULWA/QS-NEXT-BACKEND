package za.co.qsnext.employeemanagement.learning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCourseRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Size(max = 100, message = "Category must not exceed 100 characters")
        String category,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        boolean mandatory,

        UUID linkedComplianceRequirementId
) {
}
