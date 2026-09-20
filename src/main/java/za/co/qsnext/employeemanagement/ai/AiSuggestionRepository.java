package za.co.qsnext.employeemanagement.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    Page<AiSuggestion> findByRequestedByUserIdOrderByCreatedAtDesc(UUID requestedByUserId, Pageable pageable);

    Page<AiSuggestion> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(
            String subjectType, UUID subjectId, Pageable pageable);
}
