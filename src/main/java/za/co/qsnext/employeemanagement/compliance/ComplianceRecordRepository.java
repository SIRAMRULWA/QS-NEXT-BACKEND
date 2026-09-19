package za.co.qsnext.employeemanagement.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    List<ComplianceRecord> findByEmployeeId(UUID employeeId);

    boolean existsByEmployeeIdAndRequirementIdAndStatus(UUID employeeId, UUID requirementId, String status);

    List<ComplianceRecord> findByStatusAndExpiresAtBetween(
            String status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    long countByRequirementIdAndStatus(UUID requirementId, String status);
}
