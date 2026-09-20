package za.co.qsnext.employeemanagement.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, UUID> {

    Optional<IntegrationConfig> findByType(String type);
}
