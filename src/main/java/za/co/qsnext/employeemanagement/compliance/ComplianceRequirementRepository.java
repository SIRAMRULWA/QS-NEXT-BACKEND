package za.co.qsnext.employeemanagement.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, UUID> {

    boolean existsByName(String name);

    List<ComplianceRequirement> findByActiveTrue();
}
