package za.co.qsnext.employeemanagement.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PerformanceGoalRepository extends JpaRepository<PerformanceGoal, UUID> {

    List<PerformanceGoal> findByEmployeeId(UUID employeeId);

    List<PerformanceGoal> findByCycleIdAndEmployeeId(UUID cycleId, UUID employeeId);
}
