package za.co.qsnext.employeemanagement.learning.dto;

import za.co.qsnext.employeemanagement.learning.EmployeeSkill;

import java.util.UUID;

public record EmployeeSkillResponse(
        UUID id,
        UUID employeeId,
        UUID skillId,
        String proficiencyLevel
) {

    public static EmployeeSkillResponse from(EmployeeSkill employeeSkill) {
        return new EmployeeSkillResponse(
                employeeSkill.getId(),
                employeeSkill.getEmployeeId(),
                employeeSkill.getSkillId(),
                employeeSkill.getProficiencyLevel()
        );
    }
}
