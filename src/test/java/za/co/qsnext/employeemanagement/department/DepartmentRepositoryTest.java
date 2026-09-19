package za.co.qsnext.employeemanagement.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import za.co.qsnext.employeemanagement.common.AbstractRepositoryTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository-level tests for {@link DepartmentRepository}, run against a
 * real PostgreSQL instance (via Testcontainers) with Flyway migrations
 * applied, so the unique constraint on {@code departments.name} is
 * actually enforced by the database.
 */
class DepartmentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void findByName_returnsDepartment_whenNameMatches() {
        departmentRepository.saveAndFlush(new Department("Engineering", "Builds the product"));

        Optional<Department> result = departmentRepository.findByName("Engineering");

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Builds the product");
    }

    @Test
    void findByName_returnsEmpty_whenNoDepartmentMatches() {
        Optional<Department> result = departmentRepository.findByName("Nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByName_reflectsPersistedState() {
        assertThat(departmentRepository.existsByName("Sales")).isFalse();

        departmentRepository.saveAndFlush(new Department("Sales", "Sells the product"));

        assertThat(departmentRepository.existsByName("Sales")).isTrue();
    }

    @Test
    void save_rejectsDuplicateName_dueToUniqueConstraint() {
        departmentRepository.saveAndFlush(new Department("Engineering", "Builds the product"));

        assertThatThrownBy(() ->
                departmentRepository.saveAndFlush(new Department("Engineering", "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
