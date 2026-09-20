package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationId(UUID applicationId);

    List<Interview> findByInterviewerUserIdOrderByScheduledAtAsc(UUID interviewerUserId);
}
