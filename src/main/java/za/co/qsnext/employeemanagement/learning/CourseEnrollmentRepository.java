package za.co.qsnext.employeemanagement.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {

    List<CourseEnrollment> findByEmployeeId(UUID employeeId);

    Optional<CourseEnrollment> findByEmployeeIdAndCourseId(UUID employeeId, UUID courseId);

    boolean existsByEmployeeIdAndCourseId(UUID employeeId, UUID courseId);
}
