package za.co.qsnext.employeemanagement.learning;

import tools.jackson.databind.JsonNode;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 7 modules (Learning/Skills,
 * Performance, Recognition): an admin sets up a manager and their
 * report, runs a course through enrolment to completion, a full
 * self+manager performance review cycle (with the reviewer
 * auto-resolved from the Employee manager hierarchy from Phase 5), and
 * a peer recognition - all via real HTTP + the real security chain.
 */
class LearningPerformanceRecognitionIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void learningPerformanceRecognitionModules_workTogetherEndToEnd() throws Exception {
        UUID adminUserId = registerUser("hr7.admin", "hr7.admin@qsnext.co.za");
        promoteToAdmin("hr7.admin");
        String adminToken = loginAndGetAccessToken("hr7.admin", PASSWORD);

        UUID managerUserId = registerUser("hr7.manager", "hr7.manager@qsnext.co.za");
        String managerToken = loginAndGetAccessToken("hr7.manager", PASSWORD);

        UUID employeeUserId = registerUser("hr7.employee", "hr7.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr7.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Learning Dept");
        UUID managerId = createEmployeeProfile(adminToken, managerUserId, departmentId, "EMP-HR7-MGR");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId, "EMP-HR7-EMP");
        assignManager(adminToken, employeeId, managerId);

        // Learning: admin enrols the employee, employee completes it themselves.
        UUID courseId = createCourse(adminToken);
        UUID enrollmentId = enrollEmployee(adminToken, employeeId, courseId);

        mockMvc.perform(patch("/api/v1/learning/enrollments/{id}/complete", enrollmentId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.certificateIssuedAt").exists());

        // An employee cannot create courses - that needs LEARNING_MANAGE.
        mockMvc.perform(post("/api/v1/learning/courses")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unauthorized Course","mandatory":false}
                                """))
                .andExpect(status().isForbidden());

        // Performance: a review's reviewer defaults to the employee's manager;
        // once both self- and manager-assessments are submitted, it completes.
        UUID cycleId = createPerformanceCycle(adminToken);
        UUID reviewId = createReview(adminToken, cycleId, employeeId);

        mockMvc.perform(get("/api/v1/performance/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewerUserId").value(managerUserId.toString()));

        mockMvc.perform(patch("/api/v1/performance/reviews/{id}/self-assessment", reviewId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"comments":"Solid quarter"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/v1/performance/reviews/{id}/manager-assessment", reviewId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comments":"Great work"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Recognition: the employee gives their manager peer recognition.
        UUID typeId = createRecognitionType(adminToken);

        mockMvc.perform(post("/api/v1/recognition")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeId":"%s","givenToEmployeeId":"%s","message":"Thanks for the support!"}
                                """.formatted(typeId, managerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.points").value(15));

        mockMvc.perform(get("/api/v1/recognition/employees/{id}/points-total", managerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(15));
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

    private UUID createEmployeeProfile(
            String adminToken, UUID userId, UUID departmentId, String employeeNumber
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                                userId, departmentId, employeeNumber, "Jane", "Doe",
                                "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void assignManager(String adminToken, UUID employeeId, UUID managerId) throws Exception {
        mockMvc.perform(patch("/api/v1/employees/{employeeId}/manager", employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("managerId", managerId.toString()))
                .andExpect(status().isOk());
    }

    private UUID createCourse(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/learning/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Workplace Safety HR7","description":"Desc","mandatory":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID enrollEmployee(String adminToken, UUID employeeId, UUID courseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/learning/employees/{employeeId}/enrollments", employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("courseId", courseId.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createPerformanceCycle(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/performance/cycles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"2026 H1 HR7","startDate":"2026-01-01","endDate":"2026-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createReview(String adminToken, UUID cycleId, UUID employeeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/performance/reviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleId":"%s","employeeId":"%s"}
                                """.formatted(cycleId, employeeId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createRecognitionType(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recognition/types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Above and Beyond HR7","description":"Desc","pointValue":15}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
