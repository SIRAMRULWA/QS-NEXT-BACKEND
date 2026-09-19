package za.co.qsnext.employeemanagement.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    boolean existsByName(String name);
}
