package za.co.qsnext.employeemanagement.learning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AssignSkillRequest(

        @NotNull(message = "Skill ID is required")
        UUID skillId,

        @NotNull(message = "Proficiency level is required")
        @Pattern(
                regexp = "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT",
                message = "Proficiency level must be one of BEGINNER, INTERMEDIATE, ADVANCED, EXPERT"
        )
        String proficiencyLevel
) {
}
