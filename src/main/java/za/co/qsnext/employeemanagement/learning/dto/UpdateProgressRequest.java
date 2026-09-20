package za.co.qsnext.employeemanagement.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgressRequest(

        @NotNull(message = "Progress percent is required")
        @Min(value = 0, message = "Progress percent cannot be negative")
        @Max(value = 100, message = "Progress percent cannot exceed 100")
        Integer progressPercent
) {
}
