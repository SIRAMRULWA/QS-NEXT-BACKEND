package za.co.qsnext.employeemanagement.integration.dto;

import jakarta.validation.constraints.Size;

public record UpsertIntegrationSettingRequest(

        @Size(max = 500, message = "Setting value must not exceed 500 characters")
        String settingValue
) {
}
