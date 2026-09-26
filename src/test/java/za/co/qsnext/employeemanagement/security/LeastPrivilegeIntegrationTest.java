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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks in the least-privilege role model: a self-signup is an APPLICANT
 * with no access to staff data; an EMPLOYEE only reaches their own records;
 * a MANAGER approves only their direct reports; and only HR/admin can list
 * staff or allocate leave.
 */
class LeastPrivilegeIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void selfSignupIsAnApplicantWithNoAccessToStaffData() throws Exception {
        registerUser("lp.applicant1", "lp.applicant1@qsnext.co.za");
        String applicantToken = login("lp.applicant1");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("APPLICANT"))
                .andExpect(jsonPath("$.emailVerified").value(false));

        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/directory/employees").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/departments").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/careers/openings").header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotListStaffOrAllocateLeaveOrCancelSomeoneElsesLeave() throws Exception {
        String adminToken = registerAdmin("lp.admin2");
        UUID departmentId = createDepartment(adminToken, "LP Dept 2");

        UUID ownerUserId = registerUser("lp.owner2", "lp.owner2@qsnext.co.za");
        UUID ownerEmployeeId = createEmployee(adminToken, ownerUserId, departmentId, "LP-O2");
        String ownerToken = login("lp.owner2");

        UUID otherUserId = registerUser("lp.other2", "lp.other2@qsnext.co.za");
        UUID otherEmployeeId = createEmployee(adminToken, otherUserId, departmentId, "LP-X2");
        String otherToken = login("lp.other2");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/self-service/profile").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/leave/balances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceJson(ownerEmployeeId, LocalDate.now().getYear() + 1)))
                .andExpect(status().isForbidden());

        LocalDate start = LocalDate.now().plusDays(10);
        createBalance(adminToken, ownerEmployeeId, start.getYear());
        UUID leaveId = createLeave(ownerToken, ownerEmployeeId, start);

        // Filing leave for someone else, or cancelling it, is refused.
        mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson(ownerEmployeeId, start)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/leave/{id}/cancel", leaveId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // Own balances are visible.
        mockMvc.perform(get("/api/v1/leave/my-balances")
                        .param("year", String.valueOf(start.getYear()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocatedDays").value(20));

        assertThat(otherEmployeeId).isNotNull();
    }

    @Test
    void managerApprovesOnlyTheirDirectReports() throws Exception {
        String adminToken = registerAdmin("lp.admin3");
        UUID departmentId = createDepartment(adminToken, "LP Dept 3");

        UUID managerUserId = registerUser("lp.manager3", "lp.manager3@qsnext.co.za");
        UUID managerEmployeeId = createEmployee(adminToken, managerUserId, departmentId, "LP-M3");

        UUID reportUserId = registerUser("lp.report3", "lp.report3@qsnext.co.za");
        UUID reportEmployeeId = createEmployee(adminToken, reportUserId, departmentId, "LP-R3");

        UUID strangerUserId = registerUser("lp.stranger3", "lp.stranger3@qsnext.co.za");
        UUID strangerEmployeeId = createEmployee(adminToken, strangerUserId, departmentId, "LP-S3");

        mockMvc.perform(patch("/api/v1/employees/{id}/manager", reportEmployeeId)
                        .param("managerId", managerEmployeeId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String managerToken = login("lp.manager3");
        String reportToken = login("lp.report3");
        String strangerToken = login("lp.stranger3");

        LocalDate start = LocalDate.now().plusDays(20);
        createBalance(adminToken, reportEmployeeId, start.getYear());
        createBalance(adminToken, strangerEmployeeId, start.getYear());
        UUID reportLeave = createLeave(reportToken, reportEmployeeId, start);
        UUID strangerLeave = createLeave(strangerToken, strangerEmployeeId, start);

        mockMvc.perform(get("/api/v1/team").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.pendingLeave[0].id").value(reportLeave.toString()));

        mockMvc.perform(post("/api/v1/leave/{id}/approve", strangerLeave)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // A report can't approve their own request either.
        mockMvc.perform(post("/api/v1/leave/{id}/approve", reportLeave)
                        .header("Authorization", "Bearer " + reportToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/leave/{id}/approve", reportLeave)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void applicantMustVerifyEmailBeforeApplyingAndCanTrackOwnApplication() throws Exception {
        String adminToken = registerAdmin("lp.admin4");
        UUID departmentId = createDepartment(adminToken, "LP Dept 4");
        UUID postingId = createPosting(adminToken, departmentId);

        registerUser("lp.applicant4", "lp.applicant4@qsnext.co.za");
        String applicantToken = login("lp.applicant4");

        String applyJson = """
                {"firstName":"Ada","lastName":"Applicant","phone":"0123456789"}
                """;

        mockMvc.perform(post("/api/v1/careers/openings/{id}/apply", postingId)
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyJson))
                .andExpect(status().is4xxClientError());

        User applicant = userRepository.findByUsername("lp.applicant4").orElseThrow();
        applicant.markEmailVerified();
        userRepository.saveAndFlush(applicant);

        mockMvc.perform(post("/api/v1/careers/openings/{id}/apply", postingId)
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));

        mockMvc.perform(get("/api/v1/careers/my-applications")
                        .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobTitle").value("LP Engineer"));

        // HR's recruitment endpoints stay closed to applicants.
        mockMvc.perform(get("/api/v1/recruitment/postings")
                        .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void terminatingAnEmployeeBlocksTheirLogin() throws Exception {
        String adminToken = registerAdmin("lp.admin5");
        UUID departmentId = createDepartment(adminToken, "LP Dept 5");

        UUID leaverUserId = registerUser("lp.leaver5", "lp.leaver5@qsnext.co.za");
        UUID leaverEmployeeId = createEmployee(adminToken, leaverUserId, departmentId, "LP-L5");
        String leaverToken = login("lp.leaver5");

        mockMvc.perform(patch("/api/v1/employees/{id}/status", leaverEmployeeId)
                        .param("status", "TERMINATED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + leaverToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("lp.leaver5", PASSWORD))))
                .andExpect(status().is4xxClientError());
    }

    private UUID registerUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, email, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("userId").asText());
    }

    private String registerAdmin(String username) throws Exception {
        registerUser(username, username + "@qsnext.co.za");
        User user = userRepository.findByUsername(username).orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        user.assignRole(adminRole);
        userRepository.saveAndFlush(user);
        return login(username);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private UUID createDepartment(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDepartmentRequest(name, "Description"))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createEmployee(String adminToken, UUID userId, UUID departmentId, String number) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                                userId, departmentId, number, "Jane", "Doe",
                                "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void createBalance(String adminToken, UUID employeeId, int year) throws Exception {
        mockMvc.perform(post("/api/v1/leave/balances")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceJson(employeeId, year)))
                .andExpect(status().isCreated());
    }

    private UUID createLeave(String token, UUID employeeId, LocalDate start) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson(employeeId, start)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createPosting(String adminToken, UUID departmentId) throws Exception {
        MvcResult requisition = mockMvc.perform(post("/api/v1/recruitment/requisitions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"LP Engineer","departmentId":"%s","description":"Desc","numberOfOpenings":1}
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requisitionId = UUID.fromString(
                objectMapper.readTree(requisition.getResponse().getContentAsString()).get("id").asText());

        MvcResult posting = mockMvc.perform(post("/api/v1/recruitment/requisitions/{id}/postings", requisitionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"LP Engineer","description":"Desc","location":"Remote","employmentType":"FULL_TIME"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(posting.getResponse().getContentAsString()).get("id").asText());
    }

    private static String balanceJson(UUID employeeId, int year) {
        return """
                {"employeeId":"%s","leaveType":"ANNUAL","leaveYear":%d,"allocatedDays":20}
                """.formatted(employeeId, year);
    }

    private static String leaveJson(UUID employeeId, LocalDate start) {
        return """
                {"employeeId":"%s","leaveType":"ANNUAL","startDate":"%s","endDate":"%s","reason":"LP test"}
                """.formatted(employeeId, start, start.plusDays(2));
    }
}
