package za.co.qsnext.employeemanagement.security;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression coverage for the object-level authorization ("IDOR") fix on
 * Leave/Attendance/Timesheet read-by-id endpoints: an EMPLOYEE must not be
 * able to read another employee's record just because both hold the same
 * *_READ authority, while the record's own employee and an ADMIN must
 * still be able to read it.
 */
class IdorProtectionIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void employeeCannotReadAnotherEmployeesLeaveRequestByIdButOwnerAndAdminCan() throws Exception {
        UUID adminUserId = registerUser("idor.admin1", "idor.admin1@qsnext.co.za");
        promoteToAdmin("idor.admin1");
        String adminToken = loginAndGetAccessToken("idor.admin1", PASSWORD);

        UUID ownerUserId = registerUser("idor.owner1", "idor.owner1@qsnext.co.za");
        String ownerToken = loginAndGetAccessToken("idor.owner1", PASSWORD);

        UUID intruderUserId = registerUser("idor.intruder1", "idor.intruder1@qsnext.co.za");
        String intruderToken = loginAndGetAccessToken("idor.intruder1", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "IDOR Leave Dept");
        UUID ownerEmployeeId = createEmployeeProfile(adminToken, ownerUserId, departmentId, "EMP-IDOR-L01");
        createEmployeeProfile(adminToken, intruderUserId, departmentId, "EMP-IDOR-L02");

        LocalDate leaveStart = LocalDate.now().plusDays(10);
        createLeaveBalance(adminToken, ownerEmployeeId, "ANNUAL", leaveStart.getYear());
        UUID leaveRequestId = createLeaveRequest(ownerToken, ownerEmployeeId, leaveStart);

        mockMvc.perform(get("/api/v1/leave/{id}", leaveRequestId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/leave/{id}", leaveRequestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/leave/{id}", leaveRequestId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotReadAnotherEmployeesAttendanceRecordByIdButOwnerAndAdminCan() throws Exception {
        UUID adminUserId = registerUser("idor.admin2", "idor.admin2@qsnext.co.za");
        promoteToAdmin("idor.admin2");
        String adminToken = loginAndGetAccessToken("idor.admin2", PASSWORD);

        UUID ownerUserId = registerUser("idor.owner2", "idor.owner2@qsnext.co.za");
        String ownerToken = loginAndGetAccessToken("idor.owner2", PASSWORD);

        UUID intruderUserId = registerUser("idor.intruder2", "idor.intruder2@qsnext.co.za");
        String intruderToken = loginAndGetAccessToken("idor.intruder2", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "IDOR Attendance Dept");
        UUID ownerEmployeeId = createEmployeeProfile(adminToken, ownerUserId, departmentId, "EMP-IDOR-A01");
        createEmployeeProfile(adminToken, intruderUserId, departmentId, "EMP-IDOR-A02");

        UUID attendanceId = createAttendanceRecord(adminToken, ownerEmployeeId);

        mockMvc.perform(get("/api/v1/attendance/{id}", attendanceId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/attendance/{id}", attendanceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/attendance/{id}", attendanceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotReadAnotherEmployeesTimesheetByIdButOwnerAndAdminCan() throws Exception {
        UUID adminUserId = registerUser("idor.admin3", "idor.admin3@qsnext.co.za");
        promoteToAdmin("idor.admin3");
        String adminToken = loginAndGetAccessToken("idor.admin3", PASSWORD);

        UUID ownerUserId = registerUser("idor.owner3", "idor.owner3@qsnext.co.za");
        String ownerToken = loginAndGetAccessToken("idor.owner3", PASSWORD);

        UUID intruderUserId = registerUser("idor.intruder3", "idor.intruder3@qsnext.co.za");
        String intruderToken = loginAndGetAccessToken("idor.intruder3", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "IDOR Timesheet Dept");
        UUID ownerEmployeeId = createEmployeeProfile(adminToken, ownerUserId, departmentId, "EMP-IDOR-T01");
        createEmployeeProfile(adminToken, intruderUserId, departmentId, "EMP-IDOR-T02");

        UUID timesheetId = createTimesheet(ownerToken, ownerEmployeeId);

        mockMvc.perform(get("/api/v1/timesheets/{id}", timesheetId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/timesheets/{id}", timesheetId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/timesheets/{id}", timesheetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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

    private void createLeaveBalance(String adminToken, UUID employeeId, String leaveType, int leaveYear) throws Exception {
        mockMvc.perform(post("/api/v1/leave/balances")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","leaveType":"%s","leaveYear":%d,"allocatedDays":20}
                                """.formatted(employeeId, leaveType, leaveYear)))
                .andExpect(status().isCreated());
    }

    private UUID createLeaveRequest(String employeeToken, UUID employeeId, LocalDate startDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","leaveType":"ANNUAL","startDate":"%s","endDate":"%s","reason":"IDOR test"}
                                """.formatted(
                                employeeId,
                                startDate,
                                startDate.plusDays(2))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createAttendanceRecord(String adminToken, UUID employeeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","attendanceDate":"%s"}
                                """.formatted(employeeId, LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createTimesheet(String employeeToken, UUID employeeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/timesheets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":"%s","periodStart":"%s","periodEnd":"%s"}
                                """.formatted(
                                employeeId,
                                LocalDate.now().minusDays(7),
                                LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
