package za.co.qsnext.employeemanagement.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, UUID> {

    boolean existsByName(String name);
}
