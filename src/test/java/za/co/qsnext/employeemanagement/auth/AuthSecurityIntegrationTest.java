package za.co.qsnext.employeemanagement.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the Phase 1 security hardening: registration,
 * login, protected-resource authorization, refresh-token rotation,
 * logout revocation and auth-endpoint rate limiting. Runs against real
 * PostgreSQL/RabbitMQ/Redis via Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerThenLoginIssuesUsableTokens() throws Exception {

        String username = uniqueUsername();

        register(username, "Password123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "Password123"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = token(loginResult, "accessToken");

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"currentPassword":"WrongPassword1","newPassword":"NewPassword1"}
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "TotallyWrongPassword1"))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {

        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsInsufficientAuthority() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "Password123"))
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = token(loginResult, "accessToken");

        /*
         * A freshly-registered user only holds the EMPLOYEE role, which
         * is not granted USER_READ.
         */
        mockMvc.perform(
                        get("/api/v1/users/" + UUID.randomUUID())
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void refreshTokenIsSingleUse() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "Password123"))
                )
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = token(loginResult, "refreshToken");

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshJson(refreshToken))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        /*
         * Replaying the same refresh token must fail: rotation revokes
         * it as soon as it is used once.
         */
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshJson(refreshToken))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "Password123"))
                )
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = token(loginResult, "refreshToken");

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshJson(refreshToken))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshJson(refreshToken))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownPasswordCanBeChangedAndOldRefreshTokenStopsWorking() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "Password123"))
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = token(loginResult, "accessToken");
        String refreshToken = token(loginResult, "refreshToken");

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"currentPassword":"Password123","newPassword":"NewPassword1"}
                                        """)
                )
                .andExpect(status().isNoContent());

        /*
         * Changing the password revokes every refresh token issued
         * before the change.
         */
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshJson(refreshToken))
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "NewPassword1"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpointIsRateLimitedPerClient() throws Exception {

        String username = uniqueUsername();
        register(username, "Password123");

        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginJson(username, "WrongPassword1"))
                                    .with(request -> {
                                        request.setRemoteAddr("198.51.100.42");
                                        return request;
                                    })
                    )
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(username, "WrongPassword1"))
                                .with(request -> {
                                    request.setRemoteAddr("198.51.100.42");
                                    return request;
                                })
                )
                .andExpect(status().isTooManyRequests());
    }

    private void register(String username, String password) throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerJson(username, password))
                )
                .andExpect(status().isCreated());
    }

    private String registerJson(String username, String password) {
        return """
                {"username":"%s","email":"%s@example.com","password":"%s"}
                """.formatted(username, username, password);
    }

    private String loginJson(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private String refreshJson(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private String uniqueUsername() {
        return "sectest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String token(MvcResult result, String field) throws Exception {
        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$." + field
        );
    }
}
