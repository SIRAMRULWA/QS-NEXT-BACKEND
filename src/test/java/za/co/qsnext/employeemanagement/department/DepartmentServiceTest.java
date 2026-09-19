package za.co.qsnext.employeemanagement.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link DepartmentService}: the repository is mocked
 * so these run without Spring context or a database, exercising only the
 * service's business logic (duplicate-name validation, not-found handling).
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository);
    }

    @Test
    void getById_returnsDepartment_whenItExists() {
        Department department = new Department("Engineering", "Builds the product");
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));

        Department result = departmentService.getById(departmentId);

        assertThat(result).isSameAs(department);
    }

    @Test
    void getById_throwsNotFound_whenDepartmentIsMissing() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(departmentId))
                .isInstanceOf(DepartmentNotFoundException.class)
                .hasMessageContaining(departmentId.toString());
    }

    @Test
    void create_savesDepartment_whenNameIsUnique() {
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Department result = departmentService.create("Engineering", "Builds the product");

        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getDescription()).isEqualTo("Builds the product");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void create_throwsDuplicate_whenNameAlreadyExists() {
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create("Engineering", "Builds the product"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void update_throwsDuplicate_whenRenamingToAnExistingDepartment() {
        UUID departmentId = UUID.randomUUID();
        Department department = new Department("Engineering", "Builds the product");
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(departmentRepository.existsByName("Sales")).thenReturn(true);

        assertThatThrownBy(() ->
                departmentService.update(departmentId, "Sales", "Sells the product"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_allowsKeepingTheSameName() {
        UUID departmentId = UUID.randomUUID();
        Department department = new Department("Engineering", "Builds the product");
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));

        Department result = departmentService.update(departmentId, "Engineering", "Updated description");

        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(departmentRepository, never()).existsByName(anyString());
    }

    @Test
    void delete_removesDepartment_whenItExists() {
        UUID departmentId = UUID.randomUUID();
        Department department = new Department("Engineering", "Builds the product");
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));

        departmentService.delete(departmentId);

        verify(departmentRepository).delete(department);
    }

    @Test
    void delete_throwsNotFound_whenDepartmentIsMissing() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(departmentId))
                .isInstanceOf(DepartmentNotFoundException.class);

        verify(departmentRepository, never()).delete(any(Department.class));
    }
}
