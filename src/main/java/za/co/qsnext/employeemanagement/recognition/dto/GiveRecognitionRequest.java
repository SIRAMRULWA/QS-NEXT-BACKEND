package za.co.qsnext.employeemanagement.recognition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GiveRecognitionRequest(

        @NotNull(message = "Recognition type ID is required")
        UUID typeId,

        @NotNull(message = "Recipient employee ID is required")
        UUID givenToEmployeeId,

        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message,

        @Pattern(regexp = "PUBLIC|PRIVATE", message = "Visibility must be PUBLIC or PRIVATE")
        String visibility
) {
}
