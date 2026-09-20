package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByCandidateId(UUID candidateId);

    List<Application> findByJobPostingIdOrderByAppliedAtAsc(UUID jobPostingId);

    boolean existsByCandidateIdAndJobPostingId(UUID candidateId, UUID jobPostingId);

    @Query("select a.status as status, count(a) as applicationCount from Application a group by a.status")
    List<ApplicationStatusCount> countGroupedByStatus();
}
