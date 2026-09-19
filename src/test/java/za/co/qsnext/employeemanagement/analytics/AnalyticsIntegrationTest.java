package za.co.qsnext.employeemanagement.analytics;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.dto.CreateDepartmentRequest;
import za.co.qsnext.employeemanagement.employee.dto.CreateEmployeeRequest;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 11 Analytics module: an employee is
 * denied access, while an admin can pull an org-wide headcount and
 * department-distribution dashboard that reflects a just-created
 * employee - real HTTP + the real security chain, real SQL-level
 * aggregation underneath.
 */
class AnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void analyticsModule_isRestrictedToManagementAndReflectsRealData() throws Exception {
        UUID adminUserId = registerUser("hr11.admin", "hr11.admin@qsnext.co.za");
        promoteToAdmin("hr11.admin");
        String adminToken = loginAndGetAccessToken("hr11.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr11.employee", "hr11.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr11.employee", PASSWORD);

        // An employee has no ANALYTICS_READ authority.
        mockMvc.perform(get("/api/v1/analytics/headcount")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        UUID departmentId = createDepartment(adminToken, "Analytics Dept");
        createEmployeeProfile(adminToken, employeeUserId, departmentId);

        MvcResult headcountResult = mockMvc.perform(get("/api/v1/analytics/headcount")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode headcountBody = objectMapper.readTree(headcountResult.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(headcountBody.get("totalEmployees").asLong()).isGreaterThanOrEqualTo(1);

        MvcResult distributionResult = mockMvc.perform(get("/api/v1/analytics/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(distributionResult.getResponse().getContentAsString());
        boolean containsDepartment = false;
        for (JsonNode department : body.get("departments")) {
            if (departmentId.toString().equals(department.get("departmentId").asText())) {
                containsDepartment = true;
                org.assertj.core.api.Assertions.assertThat(department.get("departmentName").asText())
                        .isEqualTo("Analytics Dept");
            }
        }
        org.assertj.core.api.Assertions.assertThat(containsDepartment).isTrue();

        // Turnover, attendance and leave trends all require an explicit date range.
        mockMvc.perform(get("/api/v1/analytics/turnover")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationMethodology").isNotEmpty());

        // Report endpoints (the pre-existing, per-employee-scoped module) are gated
        // by REPORT_READ, not ANALYTICS_READ - an admin has both, an employee neither.
        mockMvc.perform(get("/api/v1/reports/departments/{id}", departmentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount").value(1));

        mockMvc.perform(get("/api/v1/reports/departments/{id}", departmentId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    private UUID registerUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("userId").asText());
    }

    private void promoteToAdmin(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Role adminRole = roleRepository.findByName(ADMIN_ROLE).orElseThrow();
        user.assignRole(adminRole);
        userRepository.saveAndFlush(user);
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private UUID createDepartment(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateDepartmentRequest(name, "Description"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createEmployeeProfile(String adminToken, UUID userId, UUID departmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                                userId, departmentId, "EMP-HR11-" + userId.toString().substring(0, 8),
                                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
