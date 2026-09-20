package za.co.qsnext.employeemanagement.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, UUID> {

    List<LearningPathCourse> findByLearningPathIdOrderBySortOrderAsc(UUID learningPathId);
}
