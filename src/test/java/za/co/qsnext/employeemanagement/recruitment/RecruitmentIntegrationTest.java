package za.co.qsnext.employeemanagement.recruitment;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.dto.CreateDepartmentRequest;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 9 Recruitment/ATS module: a
 * requisition becomes a posting, a candidate applies, is interviewed
 * (with feedback) and offered the role, and accepting the offer converts
 * them into a real Employee with an onboarding workflow started - the
 * full Recruitment -&gt; Candidate -&gt; Application -&gt; Interview -&gt; Offer
 * -&gt; Onboarding -&gt; Employee chain, via real HTTP + the real security
 * chain.
 */
class RecruitmentIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void recruitmentModule_carriesACandidateThroughToHireAndOnboarding() throws Exception {
        UUID adminUserId = registerUser("hr9.admin", "hr9.admin@qsnext.co.za");
        promoteToAdmin("hr9.admin");
        String adminToken = loginAndGetAccessToken("hr9.admin", PASSWORD);

        UUID interviewerUserId = registerUser("hr9.interviewer", "hr9.interviewer@qsnext.co.za");
        String interviewerToken = loginAndGetAccessToken("hr9.interviewer", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Engineering HR9");

        // A plain employee cannot create a requisition - that needs RECRUITMENT_MANAGE.
        mockMvc.perform(post("/api/v1/recruitment/requisitions")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unauthorized","departmentId":"%s","numberOfOpenings":1}
                                """.formatted(departmentId)))
                .andExpect(status().isForbidden());

        UUID requisitionId = createRequisition(adminToken, departmentId);
        UUID postingId = createPosting(adminToken, requisitionId);
        UUID candidateId = createCandidate(adminToken);
        UUID applicationId = createApplication(adminToken, candidateId, postingId);

        UUID interviewId = scheduleInterview(adminToken, applicationId, interviewerUserId);

        mockMvc.perform(get("/api/v1/recruitment/interviews/my-interviews")
                        .header("Authorization", "Bearer " + interviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(interviewId.toString()));

        mockMvc.perform(patch("/api/v1/recruitment/interviews/{id}/complete", interviewId)
                        .header("Authorization", "Bearer " + interviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/recruitment/interviews/{id}/feedback", interviewId)
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comments":"Excellent","recommendation":"HIRE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendation").value("HIRE"));

        UUID offerId = createOffer(adminToken, applicationId);

        mockMvc.perform(patch("/api/v1/recruitment/offers/{id}/send", offerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        mockMvc.perform(patch("/api/v1/recruitment/offers/{id}/accept", offerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        UUID onboardingTemplateId = createOnboardingTemplate(adminToken);

        MvcResult hireResult = mockMvc.perform(post("/api/v1/recruitment/offers/{id}/hire", offerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"hr9.newhire","employeeNumber":"EMP-HR9-001",
                                 "departmentId":"%s","hireDate":"%s","onboardingTemplateId":"%s"}
                                """.formatted(departmentId, LocalDate.now(), onboardingTemplateId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.onboardingStarted").value(true))
                .andReturn();

        JsonNode hireBody = objectMapper.readTree(hireResult.getResponse().getContentAsString());
        UUID employeeId = UUID.fromString(hireBody.get("employeeId").asText());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-HR9-001"));

        mockMvc.perform(get("/api/v1/onboarding/employees/{id}/workflows", employeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
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

    private UUID createRequisition(String adminToken, UUID departmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/requisitions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer HR9","departmentId":"%s",
                                 "description":"Desc","numberOfOpenings":1}
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createPosting(String adminToken, UUID requisitionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/requisitions/{id}/postings", requisitionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer HR9","description":"Desc",
                                 "location":"Remote","employmentType":"FULL_TIME"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createCandidate(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/candidates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sam","lastName":"Candidate",
                                 "email":"sam.candidate.hr9@example.com","phone":"0123456789","source":"referral"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createApplication(String adminToken, UUID candidateId, UUID postingId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/applications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("candidateId", candidateId.toString())
                        .param("jobPostingId", postingId.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID scheduleInterview(String adminToken, UUID applicationId, UUID interviewerUserId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/interviews")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationId":"%s","interviewerUserId":"%s",
                                 "scheduledAt":"%s","durationMinutes":45,"location":"Zoom"}
                                """.formatted(applicationId, interviewerUserId, OffsetDateTime.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createOffer(String adminToken, UUID applicationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recruitment/offers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationId":"%s","jobTitle":"Backend Engineer","salaryAmount":650000,
                                 "currency":"ZAR","startDate":"%s"}
                                """.formatted(applicationId, LocalDate.now().plusMonths(1))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createOnboardingTemplate(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/onboarding/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Hire Onboarding HR9",
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
}
