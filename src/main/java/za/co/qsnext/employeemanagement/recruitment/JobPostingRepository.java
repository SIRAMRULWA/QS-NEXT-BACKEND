package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    List<JobPosting> findByStatus(String status);

    List<JobPosting> findByRequisitionId(UUID requisitionId);
}
