package za.co.qsnext.employeemanagement.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.exception.IntegrationNotFoundException;
import za.co.qsnext.employeemanagement.integration.dto.IntegrationConfigResponse;
import za.co.qsnext.employeemanagement.integration.dto.IntegrationSettingResponse;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock
    private IntegrationConfigRepository configRepository;
    @Mock
    private IntegrationSettingRepository settingRepository;
    @Mock
    private AuditService auditService;

    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(configRepository, settingRepository, auditService);
    }

    private IntegrationConfig configWithId(UUID id, String type, boolean enabled) {
        IntegrationConfig config = new IntegrationConfig(type, "internal", enabled);
        setId(config, id);
        return config;
    }

    @Test
    void getConfig_throws_whenTypeIsUnknown() {
        when(configRepository.findByType("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.getConfig("UNKNOWN"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    void updateConfig_updatesProviderNameAndEnabledFlag() {
        UUID id = UUID.randomUUID();
        IntegrationConfig config = configWithId(id, IntegrationConfig.TYPE_PAYROLL, false);

        when(configRepository.findByType(IntegrationConfig.TYPE_PAYROLL)).thenReturn(Optional.of(config));

        IntegrationConfigResponse response =
                integrationService.updateConfig(IntegrationConfig.TYPE_PAYROLL, "acme-payroll", true);

        assertThat(response.providerName()).isEqualTo("acme-payroll");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void upsertSetting_createsANewSetting_whenNoneExists() {
        UUID configId = UUID.randomUUID();
        IntegrationConfig config = configWithId(configId, IntegrationConfig.TYPE_EMAIL, true);

        when(configRepository.findByType(IntegrationConfig.TYPE_EMAIL)).thenReturn(Optional.of(config));
        when(settingRepository.findByIntegrationConfigIdAndSettingKey(configId, "from-name"))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any())).thenAnswer(invocation -> {
            IntegrationSetting setting = invocation.getArgument(0);
            setId(setting, UUID.randomUUID());
            return setting;
        });

        IntegrationSettingResponse response =
                integrationService.upsertSetting(IntegrationConfig.TYPE_EMAIL, "from-name", "QSNext HR");

        assertThat(response.settingValue()).isEqualTo("QSNext HR");
    }

    @Test
    void upsertSetting_updatesAnExistingSetting() {
        UUID configId = UUID.randomUUID();
        IntegrationConfig config = configWithId(configId, IntegrationConfig.TYPE_EMAIL, true);
        IntegrationSetting existing = new IntegrationSetting(configId, "from-name", "Old Name");
        setId(existing, UUID.randomUUID());

        when(configRepository.findByType(IntegrationConfig.TYPE_EMAIL)).thenReturn(Optional.of(config));
        when(settingRepository.findByIntegrationConfigIdAndSettingKey(configId, "from-name"))
                .thenReturn(Optional.of(existing));
        when(settingRepository.save(existing)).thenReturn(existing);

        IntegrationSettingResponse response =
                integrationService.upsertSetting(IntegrationConfig.TYPE_EMAIL, "from-name", "New Name");

        assertThat(response.settingValue()).isEqualTo("New Name");
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
