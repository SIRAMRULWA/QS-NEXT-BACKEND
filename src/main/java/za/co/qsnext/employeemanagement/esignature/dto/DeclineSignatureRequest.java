package za.co.qsnext.employeemanagement.esignature.dto;

import jakarta.validation.constraints.Size;

public record DeclineSignatureRequest(

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
