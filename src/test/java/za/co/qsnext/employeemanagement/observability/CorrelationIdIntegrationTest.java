package za.co.qsnext.employeemanagement.observability;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms CorrelationIdFilter genuinely covers the whole request -
 * including ones Spring Security itself rejects before any controller
 * runs - and that the same ID makes it all the way into the audit
 * trail a login writes.
 */
class CorrelationIdIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void unauthenticatedRequest_stillGetsACorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/v1/audit/user/{userId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME))
                                .isNotBlank());
    }

    @Test
    void callerSuppliedCorrelationId_isEchoedBackUnchanged() throws Exception {
        String callerCorrelationId = "test-correlation-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/audit/user/{userId}", UUID.randomUUID())
                        .header(CorrelationIdFilter.HEADER_NAME, callerCorrelationId))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME))
                                .isEqualTo(callerCorrelationId));
    }

    @Test
    void correlationIdFromALogin_isStampedOntoItsAuditRecord() throws Exception {
        UUID adminUserId = registerUser("obs.admin", "obs.admin@qsnext.co.za");
        promoteToAdmin("obs.admin");

        String loginCorrelationId = "login-correlation-" + UUID.randomUUID();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .header(CorrelationIdFilter.HEADER_NAME, loginCorrelationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("obs.admin", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/audit/user/{userId}", adminUserId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[?(@.action == 'LOGIN_SUCCESS' && @.correlationId == '"
                                + loginCorrelationId + "')]").exists());
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
}
