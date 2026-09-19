package za.co.qsnext.employeemanagement.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.dto.CreateDepartmentRequest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test of the department endpoints: real security filter chain,
 * real database (via Testcontainers) and real service/repository layers.
 * Verifies both the happy path and the authentication/authorization rules
 * enforced by {@code @PreAuthorize} on {@link DepartmentController}.
 */
class DepartmentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void getById_returnsUnauthorized_whenNoCredentialsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_READ")
    void getById_returnsForbidden_whenUserLacksDepartmentReadAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_READ")
    void getById_returnsNotFound_whenDepartmentDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_READ")
    void getById_returnsDepartment_whenItExists() throws Exception {
        Department department = departmentRepository.saveAndFlush(
                new Department("Marketing", "Runs campaigns"));

        mockMvc.perform(get("/api/v1/departments/{id}", department.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Marketing"));
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_CREATE")
    void create_returnsCreated_whenRequestIsValid() throws Exception {
        CreateDepartmentRequest request =
                new CreateDepartmentRequest("Finance", "Manages the books");

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Finance"));
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_CREATE")
    void create_returnsBadRequest_whenNameIsBlank() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("", "Missing name");

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_CREATE")
    void create_returnsConflict_whenNameAlreadyExists() throws Exception {
        departmentRepository.saveAndFlush(new Department("Legal", "Handles contracts"));
        CreateDepartmentRequest request = new CreateDepartmentRequest("Legal", "Duplicate");

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }
}
