package za.co.qsnext.employeemanagement.integration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.exception.IntegrationNotFoundException;
import za.co.qsnext.employeemanagement.integration.dto.IntegrationConfigResponse;
import za.co.qsnext.employeemanagement.integration.dto.IntegrationSettingResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class IntegrationService {

    private final IntegrationConfigRepository configRepository;
    private final IntegrationSettingRepository settingRepository;
    private final AuditService auditService;

    public IntegrationService(
            IntegrationConfigRepository configRepository,
            IntegrationSettingRepository settingRepository,
            AuditService auditService
    ) {
        this.configRepository = configRepository;
        this.settingRepository = settingRepository;
        this.auditService = auditService;
    }

    public List<IntegrationConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream().map(IntegrationConfigResponse::from).toList();
    }

    public IntegrationConfigResponse getConfig(String type) {
        return IntegrationConfigResponse.from(findConfigOrThrow(type));
    }

    @Transactional
    public IntegrationConfigResponse updateConfig(String type, String providerName, boolean enabled) {

        IntegrationConfig config = findConfigOrThrow(type);
        config.update(providerName, enabled);

        auditService.log("INTEGRATION_CONFIG_UPDATED", "IntegrationConfig", config.getId(), AuditService.RESULT_SUCCESS);

        return IntegrationConfigResponse.from(config);
    }

    public List<IntegrationSettingResponse> getSettings(String type) {

        IntegrationConfig config = findConfigOrThrow(type);

        return settingRepository.findByIntegrationConfigId(config.getId()).stream()
                .map(IntegrationSettingResponse::from)
                .toList();
    }

    @Transactional
    public IntegrationSettingResponse upsertSetting(String type, String settingKey, String settingValue) {

        IntegrationConfig config = findConfigOrThrow(type);

        IntegrationSetting setting = settingRepository
                .findByIntegrationConfigIdAndSettingKey(config.getId(), settingKey)
                .orElseGet(() -> new IntegrationSetting(config.getId(), settingKey, settingValue));

        setting.updateValue(settingValue);
        IntegrationSetting saved = settingRepository.save(setting);

        auditService.log(
                "INTEGRATION_SETTING_UPDATED", "IntegrationSetting", saved.getId(), AuditService.RESULT_SUCCESS
        );

        return IntegrationSettingResponse.from(saved);
    }

    @Transactional
    public void deleteSetting(String type, String settingKey) {

        IntegrationConfig config = findConfigOrThrow(type);

        settingRepository.findByIntegrationConfigIdAndSettingKey(config.getId(), settingKey)
                .ifPresent(settingRepository::delete);
    }

    private IntegrationConfig findConfigOrThrow(String type) {
        return configRepository.findByType(type)
                .orElseThrow(() -> new IntegrationNotFoundException("Integration not found: " + type));
    }
}
