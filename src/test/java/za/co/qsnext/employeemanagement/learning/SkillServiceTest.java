package za.co.qsnext.employeemanagement.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.LearningNotFoundException;
import za.co.qsnext.employeemanagement.learning.dto.EmployeeSkillResponse;
import za.co.qsnext.employeemanagement.learning.dto.SkillResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AuditService auditService;

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(skillRepository, employeeSkillRepository, employeeRepository, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    @Test
    void createSkill_savesTheSkill() {
        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(skillRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            setId(skill, UUID.randomUUID());
            return skill;
        });

        SkillResponse response = skillService.createSkill("Java", "Programming");

        assertThat(response.name()).isEqualTo("Java");
    }

    @Test
    void createSkill_rejectsADuplicateName() {
        when(skillRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill("Java", "Programming"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void assignSkillToEmployee_isAllowed_fortheOwningEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(skillRepository.existsById(skillId)).thenReturn(true);
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId)).thenReturn(Optional.empty());
        when(employeeSkillRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            EmployeeSkill employeeSkill = invocation.getArgument(0);
            setId(employeeSkill, UUID.randomUUID());
            return employeeSkill;
        });

        EmployeeSkillResponse response = skillService.assignSkillToEmployee(
                employeeId, skillId, EmployeeSkill.LEVEL_ADVANCED, userId, false);

        assertThat(response.proficiencyLevel()).isEqualTo(EmployeeSkill.LEVEL_ADVANCED);
    }

    @Test
    void assignSkillToEmployee_upsertsAnExistingEntry() {
        UUID employeeId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        EmployeeSkill existing = new EmployeeSkill(employeeId, skillId, EmployeeSkill.LEVEL_BEGINNER);
        setId(existing, UUID.randomUUID());

        when(skillRepository.existsById(skillId)).thenReturn(true);
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId))
                .thenReturn(Optional.of(existing));
        when(employeeSkillRepository.save(existing)).thenReturn(existing);

        EmployeeSkillResponse response = skillService.assignSkillToEmployee(
                employeeId, skillId, EmployeeSkill.LEVEL_EXPERT, UUID.randomUUID(), true);

        assertThat(response.proficiencyLevel()).isEqualTo(EmployeeSkill.LEVEL_EXPERT);
    }

    @Test
    void assignSkillToEmployee_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> skillService.assignSkillToEmployee(
                employeeId, UUID.randomUUID(), EmployeeSkill.LEVEL_ADVANCED, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignSkillToEmployee_throws_whenSkillDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(skillRepository.existsById(skillId)).thenReturn(false);

        assertThatThrownBy(() -> skillService.assignSkillToEmployee(
                employeeId, skillId, EmployeeSkill.LEVEL_ADVANCED, userId, false))
                .isInstanceOf(LearningNotFoundException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
