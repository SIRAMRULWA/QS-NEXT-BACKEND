package za.co.qsnext.employeemanagement.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateIntegrationConfigRequest(

        @NotBlank(message = "Provider name is required")
        @Size(max = 100, message = "Provider name must not exceed 100 characters")
        String providerName,

        boolean enabled
) {
}
