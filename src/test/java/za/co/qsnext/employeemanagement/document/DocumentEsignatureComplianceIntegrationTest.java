package za.co.qsnext.employeemanagement.document;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 6 modules (Documents, E-Signature,
 * Compliance): an admin uploads a document for an employee, requests
 * the employee's signature on it, and assigns + completes a compliance
 * record that cites the same document as evidence - all via real HTTP
 * + the real security filter chain.
 */
class DocumentEsignatureComplianceIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void documentEsignatureComplianceModules_workTogetherEndToEnd() throws Exception {
        UUID adminUserId = registerUser("hr6.admin", "hr6.admin@qsnext.co.za");
        promoteToAdmin("hr6.admin");
        String adminToken = loginAndGetAccessToken("hr6.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr6.employee", "hr6.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr6.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Documents Dept");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId);

        // Document: admin uploads, employee can see it in their own list.
        UUID documentId = uploadDocument(adminToken, employeeId);

        mockMvc.perform(get("/api/v1/documents/my-documents")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentId.toString()))
                .andExpect(jsonPath("$[0].version").value(1));

        // An employee cannot upload documents for themselves - that needs DOCUMENT_MANAGE.
        mockMvc.perform(multipart("/api/v1/documents/employees/{employeeId}", employeeId)
                        .file(new MockMultipartFile("file", "id.pdf", "application/pdf", "x".getBytes()))
                        .param("category", "ID_DOCUMENT")
                        .param("title", "ID Document")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // E-Signature: admin requests the employee's signature; the employee accepts it,
        // which completes the request since they are the only signer.
        UUID signatureRequestId = createSignatureRequest(adminToken, documentId, employeeUserId);
        UUID signerId = getFirstSignerId(adminToken, signatureRequestId);

        mockMvc.perform(patch("/api/v1/esignature/signers/{signerId}/accept", signerId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Compliance: admin creates a requirement and assigns it to the employee;
        // the employee completes it citing the uploaded document as evidence.
        UUID requirementId = createComplianceRequirement(adminToken);
        assignComplianceRequirement(adminToken, employeeId, requirementId);

        MvcResult myRecordsResult = mockMvc.perform(get("/api/v1/compliance/my-records")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andReturn();

        JsonNode records = objectMapper.readTree(myRecordsResult.getResponse().getContentAsString());
        UUID recordId = UUID.fromString(records.get(0).get("id").asText());

        mockMvc.perform(patch("/api/v1/compliance/records/{recordId}/complete", recordId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evidenceDocumentId":"%s","notes":"Uploaded ID document"}
                                """.formatted(documentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.expiresAt").exists());
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
                                userId, departmentId, "EMP-HR6-001", "Jane", "Doe",
                                "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID uploadDocument(String adminToken, UUID employeeId) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/employees/{employeeId}", employeeId)
                        .file(new MockMultipartFile("file", "id.pdf", "application/pdf", "id-content".getBytes()))
                        .param("category", "ID_DOCUMENT")
                        .param("title", "ID Document")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createSignatureRequest(String adminToken, UUID documentId, UUID signerUserId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/esignature/requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentId":"%s","title":"Please sign your ID document",
                                 "signerUserIds":["%s"],"expiresAt":"%s"}
                                """.formatted(documentId, signerUserId, OffsetDateTime.now().plusDays(7))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID getFirstSignerId(String adminToken, UUID requestId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/esignature/requests/{requestId}", requestId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("signers").get(0).get("id").asText());
    }

    private UUID createComplianceRequirement(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/compliance/requirements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ID Document HR6","description":"Must have a valid ID on file",
                                 "category":"DOCUMENT","mandatory":true,"validityPeriodDays":365}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void assignComplianceRequirement(String adminToken, UUID employeeId, UUID requirementId) throws Exception {
        mockMvc.perform(post("/api/v1/compliance/employees/{employeeId}/records", employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("requirementId", requirementId.toString()))
                .andExpect(status().isCreated());
    }
}
