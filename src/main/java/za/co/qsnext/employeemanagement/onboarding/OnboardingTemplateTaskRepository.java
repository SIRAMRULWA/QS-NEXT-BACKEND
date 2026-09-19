package za.co.qsnext.employeemanagement.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingTemplateTaskRepository extends JpaRepository<OnboardingTemplateTask, UUID> {

    List<OnboardingTemplateTask> findByTemplateIdOrderBySortOrderAsc(UUID templateId);
}
