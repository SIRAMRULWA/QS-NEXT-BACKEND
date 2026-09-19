package za.co.qsnext.employeemanagement.integration.dto;

import za.co.qsnext.employeemanagement.integration.IntegrationConfig;

import java.util.UUID;

public record IntegrationConfigResponse(
        UUID id,
        String type,
        String providerName,
        boolean enabled
) {

    public static IntegrationConfigResponse from(IntegrationConfig config) {
        return new IntegrationConfigResponse(
                config.getId(), config.getType(), config.getProviderName(), config.isEnabled()
        );
    }
}
