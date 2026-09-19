package za.co.qsnext.employeemanagement.performance.dto;

import jakarta.validation.constraints.Size;

public record SubmitAssessmentRequest(

        Integer rating,

        @Size(max = 2000, message = "Comments must not exceed 2000 characters")
        String comments
) {
}
