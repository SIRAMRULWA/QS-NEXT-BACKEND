package za.co.qsnext.employeemanagement.directory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test of the directory endpoints: real security filter chain,
 * real database (via Testcontainers) and real service/repository layers.
 *
 * <p>Regression coverage for a bug where {@code GET /api/v1/directory/
 * employees?query=} (an empty, but non-null, query string - the "browse
 * everything" case a real frontend sends on first load) returned HTTP 500.
 * {@link DirectoryController} treats a blank query as "no filter" and
 * {@link DirectoryService} normalizes it to {@code null} before calling
 * {@link EmployeeRepository#search}; with that {@code null} bound into
 * {@code lower(concat('%', :query, '%'))}, Postgres/pgjdbc could not
 * determine the parameter's type and fell back to {@code bytea}, so
 * Postgres raised {@code function lower(bytea) does not exist}. This only
 * reproduces against a real PostgreSQL instance (via Testcontainers here),
 * not against an in-memory/mocked repository - see
 * {@code EmployeeRepository#search}'s Javadoc for the fix.
 *
 * <p>Assertions use a page large enough to hold every employee/department
 * the shared test suite may have created, and match on this test's own
 * randomly-generated identifiers, since {@link AbstractIntegrationTest}'s
 * Spring context (and its Postgres container) is reused across the whole
 * {@code *IntegrationTest} suite.
 */
class DirectoryControllerIntegrationTest extends AbstractIntegrationTest {

    // DirectoryController caps page size at 100 regardless of what's requested.
    private static final String LARGE_PAGE_SIZE = "100";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = "DIRECTORY_READ")
    void searchEmployees_returnsOk_whenQueryIsEmptyString() throws Exception {
        Department department = departmentRepository.saveAndFlush(
                new Department("Engineering-" + UUID.randomUUID(), "Builds the product"));

        User user = userRepository.saveAndFlush(new User(
                "directory.jane-" + UUID.randomUUID(),
                "directory.jane-" + UUID.randomUUID() + "@example.com",
                "hash"));

        String employeeNumber = "EMP-" + UUID.randomUUID();

        employeeRepository.saveAndFlush(new Employee(
                user.getId(), department.getId(), employeeNumber,
                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)));

        mockMvc.perform(get("/api/v1/directory/employees")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", LARGE_PAGE_SIZE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath(
                        "$.content[?(@.employeeNumber == '" + employeeNumber + "')]").exists());
    }

    @Test
    @WithMockUser(authorities = "DIRECTORY_READ")
    void searchDepartments_returnsOk_whenQueryIsEmptyString() throws Exception {
        String departmentName = "Engineering-" + UUID.randomUUID();
        departmentRepository.saveAndFlush(new Department(departmentName, "Builds the product"));

        mockMvc.perform(get("/api/v1/directory/departments")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", LARGE_PAGE_SIZE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath(
                        "$.content[?(@.name == '" + departmentName + "')]").exists());
    }
}
