package za.co.qsnext.employeemanagement.learning;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.LearningNotFoundException;
import za.co.qsnext.employeemanagement.learning.dto.EmployeeSkillResponse;
import za.co.qsnext.employeemanagement.learning.dto.SkillResponse;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public SkillService(
            SkillRepository skillRepository,
            EmployeeSkillRepository employeeSkillRepository,
            EmployeeRepository employeeRepository,
            AuditService auditService
    ) {
        this.skillRepository = skillRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SkillResponse createSkill(String name, String category) {

        if (skillRepository.existsByName(name)) {
            throw new DuplicateResourceException("Skill already exists: " + name);
        }

        return SkillResponse.from(skillRepository.save(new Skill(name, category)));
    }

    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream().map(SkillResponse::from).toList();
    }

    /**
     * Upserts the employee's proficiency for a skill - self-declared by
     * the employee, or set on their behalf by anyone with LEARNING_MANAGE.
     */
    @Transactional
    public EmployeeSkillResponse assignSkillToEmployee(
            UUID employeeId,
            UUID skillId,
            String proficiencyLevel,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanManageEmployeeSkills(employeeId, requesterUserId, requesterCanManage);

        if (!skillRepository.existsById(skillId)) {
            throw new LearningNotFoundException("Skill not found: " + skillId);
        }

        EmployeeSkill employeeSkill = employeeSkillRepository
                .findByEmployeeIdAndSkillId(employeeId, skillId)
                .orElseGet(() -> new EmployeeSkill(employeeId, skillId, proficiencyLevel));

        employeeSkill.updateProficiency(proficiencyLevel);
        EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);

        auditService.log("EMPLOYEE_SKILL_ASSIGNED", "EmployeeSkill", saved.getId(), AuditService.RESULT_SUCCESS);

        return EmployeeSkillResponse.from(saved);
    }

    public List<EmployeeSkillResponse> getEmployeeSkills(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanManageEmployeeSkills(employeeId, requesterUserId, requesterCanManage);

        return employeeSkillRepository.findByEmployeeId(employeeId).stream()
                .map(EmployeeSkillResponse::from)
                .toList();
    }

    private void assertCanManageEmployeeSkills(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to manage this employee's skills");
        }
    }
}
