package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {

    Optional<InterviewFeedback> findByInterviewId(UUID interviewId);

    boolean existsByInterviewId(UUID interviewId);
}
