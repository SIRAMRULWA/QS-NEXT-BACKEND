package za.co.qsnext.employeemanagement.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    List<PerformanceReview> findByEmployeeId(UUID employeeId);

    List<PerformanceReview> findByReviewerUserId(UUID reviewerUserId);

    boolean existsByCycleIdAndEmployeeId(UUID cycleId, UUID employeeId);
}
