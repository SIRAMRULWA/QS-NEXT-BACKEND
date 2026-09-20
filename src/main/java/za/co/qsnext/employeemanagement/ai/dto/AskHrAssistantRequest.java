package za.co.qsnext.employeemanagement.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskHrAssistantRequest(

        @NotBlank
        @Size(max = 2000)
        String question
) {
}
