package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateJobPostingRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        @NotNull(message = "Employment type is required")
        @Pattern(
                regexp = "FULL_TIME|PART_TIME|CONTRACT|TEMPORARY",
                message = "Employment type must be one of FULL_TIME, PART_TIME, CONTRACT, TEMPORARY"
        )
        String employmentType
) {
}
