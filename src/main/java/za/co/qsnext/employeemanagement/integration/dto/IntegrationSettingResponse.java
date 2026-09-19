package za.co.qsnext.employeemanagement.integration.dto;

import za.co.qsnext.employeemanagement.integration.IntegrationSetting;

import java.util.UUID;

public record IntegrationSettingResponse(UUID id, String settingKey, String settingValue) {

    public static IntegrationSettingResponse from(IntegrationSetting setting) {
        return new IntegrationSettingResponse(setting.getId(), setting.getSettingKey(), setting.getSettingValue());
    }
}
