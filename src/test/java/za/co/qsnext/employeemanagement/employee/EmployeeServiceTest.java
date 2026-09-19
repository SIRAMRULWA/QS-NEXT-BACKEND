package za.co.qsnext.employeemanagement.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.department.DepartmentService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.user.UserService;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Focused on assignManager, added in Phase 5 - EmployeeService otherwise
 * has no prior test coverage to preserve/extend here.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserService userService;
    @Mock
    private DepartmentService departmentService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, userService, departmentService);
    }

    private Employee employeeWithId(UUID id) {
        Employee employee = new Employee(
                UUID.randomUUID(), UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        try {
            Field field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return employee;
    }

    @Test
    void assignManager_setsTheManager_whenValid() {
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Employee employee = employeeWithId(employeeId);
        Employee manager = employeeWithId(managerId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));

        Employee result = employeeService.assignManager(employeeId, managerId);

        assertThat(result.getManagerId()).isEqualTo(managerId);
    }

    @Test
    void assignManager_allowsClearingTheManager() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId);
        employee.assignManager(UUID.randomUUID());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        Employee result = employeeService.assignManager(employeeId, null);

        assertThat(result.getManagerId()).isNull();
    }

    @Test
    void assignManager_rejectsSelfManagement() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.assignManager(employeeId, employeeId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assignManager_rejectsAReportingCycle() {
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Employee employee = employeeWithId(employeeId);
        Employee manager = employeeWithId(managerId);
        // The prospective manager already reports (indirectly) to the employee.
        manager.assignManager(employeeId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> employeeService.assignManager(employeeId, managerId))
                .isInstanceOf(BusinessRuleException.class);
    }
}
