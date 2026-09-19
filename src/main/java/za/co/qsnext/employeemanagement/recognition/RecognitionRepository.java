package za.co.qsnext.employeemanagement.recognition;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecognitionRepository extends JpaRepository<Recognition, UUID> {

    List<Recognition> findByGivenToEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<Recognition> findByGivenByUserIdOrderByCreatedAtDesc(UUID givenByUserId);

    @Query("select coalesce(sum(r.points), 0) from Recognition r where r.givenToEmployeeId = :employeeId")
    long sumPointsByGivenToEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("""
            select r.givenToEmployeeId as employeeId, sum(r.points) as totalPoints
            from Recognition r
            group by r.givenToEmployeeId
            order by sum(r.points) desc
            """)
    List<RecognitionLeaderboardEntry> findLeaderboard(Pageable pageable);
}
