package za.co.qsnext.employeemanagement.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingWorkflowRepository extends JpaRepository<OnboardingWorkflow, UUID> {

    List<OnboardingWorkflow> findByEmployeeId(UUID employeeId);

    boolean existsByEmployeeIdAndStatus(UUID employeeId, String status);
}
