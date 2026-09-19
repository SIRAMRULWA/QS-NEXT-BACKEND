package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateJobRequisitionRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Min(value = 1, message = "Number of openings must be at least 1")
        int numberOfOpenings
) {
}
