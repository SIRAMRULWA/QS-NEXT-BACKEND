package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveAccrualRunRepository extends JpaRepository<LeaveAccrualRun, UUID> {

    boolean existsByPolicyIdAndPeriod(UUID policyId, String period);
}
