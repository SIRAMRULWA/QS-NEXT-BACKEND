package za.co.qsnext.employeemanagement.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import za.co.qsnext.employeemanagement.common.AbstractRepositoryTest;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level tests for {@link EmployeeRepository#search}, run
 * against a real PostgreSQL instance (via Testcontainers) rather than a
 * mock, since the bug this guards against only manifests against a real
 * JDBC driver/database - a mocked repository never sends any SQL.
 *
 * <p>Regression test for a bug where a {@code null} query (the value
 * {@link za.co.qsnext.employeemanagement.directory.DirectoryService}
 * passes for "browse everything", including the empty-string query a
 * frontend sends on first load) made Postgres unable to infer the type of
 * the parameter bound inside {@code lower(concat('%', :query, '%'))}. The
 * JDBC driver fell back to binding it as {@code bytea}, and Postgres
 * raised {@code function lower(bytea) does not exist} - see
 * {@link EmployeeRepository#search}'s Javadoc for the fix.
 */
class EmployeeRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void search_withNullQuery_matchesEverything() {
        Employee employee = seedEmployee();

        Page<Employee> result = employeeRepository.search(null, PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Employee::getId)
                .contains(employee.getId());
    }

    @Test
    void search_withEmptyStringQuery_matchesEverything() {
        Employee employee = seedEmployee();

        Page<Employee> result = employeeRepository.search("", PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Employee::getId)
                .contains(employee.getId());
    }

    @Test
    void search_withNonBlankQuery_matchesByFirstName() {
        Employee employee = seedEmployee();

        Page<Employee> result = employeeRepository.search("jan", PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Employee::getId)
                .contains(employee.getId());
    }

    private Employee seedEmployee() {
        Department department = departmentRepository.saveAndFlush(
                new Department("Engineering-" + UUID.randomUUID(), "Builds the product"));

        User user = userRepository.saveAndFlush(
                new User("jane.doe-" + UUID.randomUUID(), "jane-" + UUID.randomUUID() + "@example.com", "hash"));

        return employeeRepository.saveAndFlush(new Employee(
                user.getId(), department.getId(), "EMP-" + UUID.randomUUID(),
                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)));
    }
}
