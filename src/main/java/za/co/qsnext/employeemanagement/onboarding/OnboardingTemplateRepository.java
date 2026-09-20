package za.co.qsnext.employeemanagement.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, UUID> {

    boolean existsByName(String name);
}
