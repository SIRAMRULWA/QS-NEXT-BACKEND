package za.co.qsnext.employeemanagement.calendar;

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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 5 core HR expansion (Calendar,
 * Directory, Scheduling, Onboarding): an admin sets up a department,
 * an employee profile, a holiday, a shift assignment and an onboarding
 * workflow via real HTTP + the real security filter chain, and the
 * employee is then confirmed to see (and, for onboarding, act on) only
 * what their permissions allow.
 */
class CoreHrExpansionIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void coreHrExpansionModules_workTogetherEndToEnd() throws Exception {
        UUID adminUserId = registerUser("hr5.admin", "hr5.admin@qsnext.co.za");
        promoteToAdmin("hr5.admin");
        String adminToken = loginAndGetAccessToken("hr5.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr5.employee", "hr5.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr5.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "HR Expansion Dept");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId);

        // Calendar: an admin-created holiday is visible (PUBLIC) to the employee.
        LocalDate holidayDate = LocalDate.now().plusDays(30);
        createHoliday(adminToken, "Founders Day", holidayDate);

        OffsetDateTime rangeStart = holidayDate.minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = holidayDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        mockMvc.perform(get("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("start", rangeStart.toString())
                        .param("end", rangeEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Founders Day')]").exists());

        // Directory: the employee can find their own profile with the department name resolved.
        mockMvc.perform(get("/api/v1/directory/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("query", "hr5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + employeeId + "')].departmentName")
                        .value("HR Expansion Dept"));

        // Scheduling: an admin-assigned shift shows up on the employee's own schedule,
        // and a calendar event was recorded for it as a side effect.
        UUID shiftId = createShift(adminToken, "Morning", LocalTime.of(8, 0), LocalTime.of(16, 0), departmentId);
        LocalDate workDate = LocalDate.now().plusDays(3);
        assignShift(adminToken, employeeId, shiftId, workDate);

        mockMvc.perform(get("/api/v1/scheduling/my-schedule")
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("start", workDate.toString())
                        .param("end", workDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shiftId").value(shiftId.toString()))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        // An employee cannot create shifts - SCHEDULE_MANAGE is not granted to the EMPLOYEE role.
        mockMvc.perform(post("/api/v1/scheduling/shifts")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Night","startTime":"22:00:00","endTime":"06:00:00"}
                                """))
                .andExpect(status().isForbidden());

        // Onboarding: starting a workflow from a template copies its tasks, the employee
        // sees them on their own task list, and completing the only task auto-completes
        // the workflow.
        UUID templateId = createOnboardingTemplate(adminToken);
        startOnboardingWorkflow(adminToken, employeeId, templateId);

        MvcResult myTasksResult = mockMvc.perform(get("/api/v1/onboarding/my-tasks")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Sign contract"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andReturn();

        JsonNode tasks = objectMapper.readTree(myTasksResult.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(tasks.get(0).get("id").asText());

        mockMvc.perform(patch("/api/v1/onboarding/tasks/{taskId}/complete", taskId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
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
                                userId, departmentId, "EMP-HR5-001", "Jane", "Doe",
                                "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void createHoliday(String adminToken, String title, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/v1/calendar/holidays")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","date":"%s"}
                                """.formatted(title, date)))
                .andExpect(status().isCreated());
    }

    private UUID createShift(
            String adminToken, String name, LocalTime start, LocalTime end, UUID departmentId
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scheduling/shifts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","startTime":"%s","endTime":"%s","departmentId":"%s"}
                                """.formatted(name, start, end, departmentId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void assignShift(String adminToken, UUID employeeId, UUID shiftId, LocalDate workDate) throws Exception {
        mockMvc.perform(post("/api/v1/scheduling/assignments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","shiftId":"%s","workDate":"%s"}
                                """.formatted(employeeId, shiftId, workDate)))
                .andExpect(status().isCreated());
    }

    private UUID createOnboardingTemplate(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/onboarding/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Standard Onboarding HR5",
                                  "description": "Standard checklist",
                                  "tasks": [
                                    {"title": "Sign contract", "description": "HR paperwork", "assigneeRole": "EMPLOYEE", "sortOrder": 1}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void startOnboardingWorkflow(String adminToken, UUID employeeId, UUID templateId) throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("employeeId", employeeId.toString())
                        .param("templateId", templateId.toString()))
                .andExpect(status().isCreated());
    }
}
