package za.co.qsnext.employeemanagement.audit;

import tools.jackson.databind.JsonNode;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the audit query API is actually restricted to AUDIT_READ
 * (ADMIN-only by the seed migration) and, for an admin, returns entries a
 * real request wrote - e.g. their own login.
 */
class AuditControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void getByUser_isForbidden_forANonAdminUser() throws Exception {
        registerUser("audit.employee", "audit.employee@qsnext.co.za");
        String accessToken = loginAndGetAccessToken("audit.employee", "S3curePassword!");

        mockMvc.perform(get("/api/v1/audit/user/{userId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByUser_isUnauthorized_withoutATokenAtAll() throws Exception {
        mockMvc.perform(get("/api/v1/audit/user/{userId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getByUser_returnsTheAdminsOwnLoginEvent() throws Exception {
        UUID adminUserId = registerUser("audit.admin", "audit.admin@qsnext.co.za");
        promoteToAdmin("audit.admin");

        // The registration above already logged in implicitly (register
        // issues tokens), but log in again explicitly so there is a
        // definite LOGIN_SUCCESS entry for this admin to find.
        String accessToken = loginAndGetAccessToken("audit.admin", "S3curePassword!");

        mockMvc.perform(get("/api/v1/audit/user/{userId}", adminUserId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'LOGIN_SUCCESS')]").exists());
    }

    @Test
    void getByAction_isForbidden_forANonAdminUser() throws Exception {
        registerUser("audit.employee2", "audit.employee2@qsnext.co.za");
        String accessToken = loginAndGetAccessToken("audit.employee2", "S3curePassword!");

        mockMvc.perform(get("/api/v1/audit/action/{action}", "LOGIN_SUCCESS")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    private UUID registerUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, "S3curePassword!"))))
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
