package za.co.qsnext.employeemanagement.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    List<PerformanceReview> findByEmployeeId(UUID employeeId);

    List<PerformanceReview> findByReviewerUserId(UUID reviewerUserId);

    boolean existsByCycleIdAndEmployeeId(UUID cycleId, UUID employeeId);

    long countByStatus(String status);

    @Query("select avg(r.managerRating) from PerformanceReview r where r.status = 'COMPLETED' and r.managerRating is not null")
    Double averageCompletedManagerRating();
}
