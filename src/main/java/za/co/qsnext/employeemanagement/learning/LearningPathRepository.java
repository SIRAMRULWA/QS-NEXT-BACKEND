package za.co.qsnext.employeemanagement.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    boolean existsByName(String name);
}
