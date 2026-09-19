package za.co.qsnext.employeemanagement.directory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.directory.dto.DirectoryEmployeeResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    private DirectoryService directoryService;

    @BeforeEach
    void setUp() {
        directoryService = new DirectoryService(employeeRepository, departmentRepository);
    }

    private Employee employeeWithId(UUID id, UUID departmentId, UUID managerId, String firstName) {
        Employee employee = new Employee(
                UUID.randomUUID(), departmentId, "EMP-" + id, firstName, "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setField(employee, "id", id);
        if (managerId != null) {
            employee.assignManager(managerId);
        }
        return employee;
    }

    private Department departmentWithId(UUID id, String name) {
        Department department = new Department(name, "Description");
        setField(department, "id", id);
        return department;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void searchEmployees_includesDepartmentAndManagerNames() {
        UUID departmentId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Employee manager = employeeWithId(managerId, departmentId, null, "Alex");
        Employee employee = employeeWithId(employeeId, departmentId, managerId, "Jane");
        Department department = departmentWithId(departmentId, "Engineering");

        Pageable pageable = PageRequest.of(0, 20);
        when(employeeRepository.search(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));
        when(departmentRepository.findAllById(List.of(departmentId)))
                .thenReturn(List.of(department));
        when(employeeRepository.findAllById(List.of(managerId)))
                .thenReturn(List.of(manager));

        Page<DirectoryEmployeeResponse> result = directoryService.searchEmployees("jane", pageable);

        assertThat(result.getContent()).hasSize(1);
        DirectoryEmployeeResponse response = result.getContent().getFirst();
        assertThat(response.departmentName()).isEqualTo("Engineering");
        assertThat(response.managerName()).isEqualTo("Alex Doe");
    }

    @Test
    void getEmployeeProfile_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directoryService.getEmployeeProfile(employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void getManagerChain_walksUpUntilNoManager() {
        UUID topId = UUID.randomUUID();
        UUID midId = UUID.randomUUID();
        UUID bottomId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Employee top = employeeWithId(topId, departmentId, null, "Top");
        Employee mid = employeeWithId(midId, departmentId, topId, "Mid");
        Employee bottom = employeeWithId(bottomId, departmentId, midId, "Bottom");

        when(employeeRepository.findById(bottomId)).thenReturn(Optional.of(bottom));
        when(employeeRepository.findById(midId)).thenReturn(Optional.of(mid));
        when(employeeRepository.findById(topId)).thenReturn(Optional.of(top));
        when(departmentRepository.findAllById(any())).thenReturn(List.of());

        List<DirectoryEmployeeResponse> chain = directoryService.getManagerChain(bottomId);

        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).firstName()).isEqualTo("Mid");
        assertThat(chain.get(1).firstName()).isEqualTo("Top");
    }

    @Test
    void getManagerChain_isEmpty_whenEmployeeHasNoManager() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employeeWithId(employeeId, UUID.randomUUID(), null, "Solo");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThat(directoryService.getManagerChain(employeeId)).isEmpty();
    }

    @Test
    void getDirectReports_returnsEmployeesReportingToTheGivenManager() {
        UUID managerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Employee report = employeeWithId(UUID.randomUUID(), departmentId, managerId, "Report");

        when(employeeRepository.findByManagerId(managerId)).thenReturn(List.of(report));
        when(departmentRepository.findAllById(any())).thenReturn(List.of());

        List<DirectoryEmployeeResponse> reports = directoryService.getDirectReports(managerId);

        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().firstName()).isEqualTo("Report");
    }
}
