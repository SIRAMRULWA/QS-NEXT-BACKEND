package za.co.qsnext.employeemanagement.ai;

import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.integration.IntegrationConfig;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 12 AI module: with the AI
 * integration disabled (its seeded default), even a permitted request
 * is refused with a clear business-rule error rather than silently
 * calling out to a vendor; an admin can enable it through the existing
 * generic Integration endpoints; and an employee without any AI
 * authority is forbidden outright. This test never enables a real
 * vendor call (no credentials exist in this environment) - it proves
 * the SAFE-abstraction gating, not the Anthropic HTTP call itself.
 */
class AiIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void aiModule_isPermissionGatedAndSafeByDefault() throws Exception {
        UUID adminUserId = registerUser("hr12.admin", "hr12.admin@qsnext.co.za");
        promoteToAdmin("hr12.admin");
        String adminToken = loginAndGetAccessToken("hr12.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr12.employee", "hr12.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr12.employee", PASSWORD);

        // The AI integration is seeded disabled - even with the right
        // authority, the request is refused with a clear business-rule
        // error rather than an unhandled failure.
        mockMvc.perform(post("/api/v1/ai/assistant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"How many leave days do I get?"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        // A plain employee has no AI_HR_TOOLS_USE authority.
        mockMvc.perform(post("/api/v1/ai/documents/{id}/summarize", UUID.randomUUID())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // An admin can enable the AI integration through the existing
        // generic integration-management endpoint - no AI-specific
        // toggle endpoint was needed.
        mockMvc.perform(put("/api/v1/integrations/{type}", IntegrationConfig.TYPE_AI)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerName":"anthropic","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // Reset back to disabled so this test never depends on live
        // vendor credentials existing in the environment it runs in.
        mockMvc.perform(put("/api/v1/integrations/{type}", IntegrationConfig.TYPE_AI)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerName":"anthropic","enabled":false}
                                """))
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
}
