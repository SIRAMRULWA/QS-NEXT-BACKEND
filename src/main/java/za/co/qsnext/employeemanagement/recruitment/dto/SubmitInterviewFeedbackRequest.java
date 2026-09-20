package za.co.qsnext.employeemanagement.recruitment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitInterviewFeedbackRequest(

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must not exceed 5")
        Integer rating,

        @Size(max = 2000, message = "Comments must not exceed 2000 characters")
        String comments,

        @NotNull(message = "Recommendation is required")
        @Pattern(
                regexp = "HIRE|NO_HIRE|MAYBE",
                message = "Recommendation must be one of HIRE, NO_HIRE, MAYBE"
        )
        String recommendation
) {
}
