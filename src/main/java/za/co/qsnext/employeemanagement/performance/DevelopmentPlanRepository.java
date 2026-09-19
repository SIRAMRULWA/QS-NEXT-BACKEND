package za.co.qsnext.employeemanagement.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DevelopmentPlanRepository extends JpaRepository<DevelopmentPlan, UUID> {

    List<DevelopmentPlan> findByEmployeeId(UUID employeeId);
}
