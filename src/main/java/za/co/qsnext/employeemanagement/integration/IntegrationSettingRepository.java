package za.co.qsnext.employeemanagement.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationSettingRepository extends JpaRepository<IntegrationSetting, UUID> {

    List<IntegrationSetting> findByIntegrationConfigId(UUID integrationConfigId);

    Optional<IntegrationSetting> findByIntegrationConfigIdAndSettingKey(UUID integrationConfigId, String settingKey);
}
