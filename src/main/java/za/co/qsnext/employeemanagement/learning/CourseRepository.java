package za.co.qsnext.employeemanagement.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByTitle(String title);

    List<Course> findByActiveTrue();
}
