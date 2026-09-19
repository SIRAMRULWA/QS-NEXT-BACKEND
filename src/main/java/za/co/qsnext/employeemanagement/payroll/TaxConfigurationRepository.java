package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, UUID> {

    boolean existsByName(String name);

    List<TaxConfiguration> findByActiveTrue();
}
