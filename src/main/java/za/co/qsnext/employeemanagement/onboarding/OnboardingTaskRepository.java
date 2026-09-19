package za.co.qsnext.employeemanagement.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {

    List<OnboardingTask> findByWorkflowIdOrderBySortOrderAsc(UUID workflowId);

    boolean existsByWorkflowIdAndStatusNot(UUID workflowId, String status);

    @Query("""
            select t from OnboardingTask t
            where t.workflowId in (
                select w.id from OnboardingWorkflow w where w.employeeId = :employeeId
            )
            order by t.sortOrder asc
            """)
    List<OnboardingTask> findByEmployeeId(@Param("employeeId") UUID employeeId);
}
