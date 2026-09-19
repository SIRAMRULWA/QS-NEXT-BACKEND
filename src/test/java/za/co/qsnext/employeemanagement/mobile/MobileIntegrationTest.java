package za.co.qsnext.employeemanagement.mobile;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 13 Mobile API: the composite
 * dashboard reflects a freshly created employee's real profile and
 * leave balance in one call, and device registration/listing/removal
 * round-trips over real HTTP with the real security chain.
 */
class MobileIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void mobileDashboard_reflectsRealEmployeeDataInOneCall() throws Exception {
        UUID adminUserId = registerUser("hr13.admin", "hr13.admin@qsnext.co.za");
        promoteToAdmin("hr13.admin");
        String adminToken = loginAndGetAccessToken("hr13.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr13.employee", "hr13.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr13.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Mobile Dept");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId);

        mockMvc.perform(post("/api/v1/leave/balances")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","leaveType":"ANNUAL","leaveYear":%d,"allocatedDays":21}
                                """.formatted(employeeId, LocalDate.now().getYear())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/mobile/dashboard")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.leaveBalances[0].leaveType").value("ANNUAL"))
                .andExpect(jsonPath("$.leaveBalances[0].remainingDays").value(21))
                .andExpect(jsonPath("$.todayAttendance").doesNotExist());
    }

    @Test
    void deviceRegistration_roundTripsOverRealHttp() throws Exception {
        registerUser("hr13.mobile", "hr13.mobile@qsnext.co.za");
        String token = loginAndGetAccessToken("hr13.mobile", PASSWORD);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/mobile/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceToken":"fcm-token-hr13","platform":"ANDROID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID deviceId = UUID.fromString(body.get("id").asText());

        mockMvc.perform(get("/api/v1/mobile/devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deviceId.toString()));

        mockMvc.perform(delete("/api/v1/mobile/devices/{id}", deviceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/mobile/devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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
                                userId, departmentId, "EMP-HR13-" + userId.toString().substring(0, 8),
                                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
