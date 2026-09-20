package za.co.qsnext.employeemanagement.learning.dto;

import za.co.qsnext.employeemanagement.learning.Skill;

import java.util.UUID;

public record SkillResponse(UUID id, String name, String category) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }
}
