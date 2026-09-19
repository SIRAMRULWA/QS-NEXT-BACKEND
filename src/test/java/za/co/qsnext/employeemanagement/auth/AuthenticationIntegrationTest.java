package za.co.qsnext.employeemanagement.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the register -> login -> refresh authentication
 * workflow against a real database and the real JWT signing/validation
 * pipeline, proving the whole authentication stack works together rather
 * than each piece in isolation.
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAndRefresh_issueUsableTokens() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("jane.doe", "jane.doe@qsnext.co.za", "S3curePassword!");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("jane.doe"))
                .andReturn();

        JsonNode registerBody =
                objectMapper.readTree(registerResult.getResponse().getContentAsString());
        assertThat(registerBody.get("accessToken").asText()).isNotBlank();

        LoginRequest loginRequest = new LoginRequest("jane.doe", "S3curePassword!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginBody =
                objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("jane.doe"));
    }

    @Test
    void register_rejectsDuplicateUsername() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("duplicate.user", "first@qsnext.co.za", "S3curePassword!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        RegisterRequest duplicateRequest =
                new RegisterRequest("duplicate.user", "second@qsnext.co.za", "AnotherPassword1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void login_rejectsInvalidCredentials() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("bad.login", "bad.login@qsnext.co.za", "S3curePassword!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("bad.login", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void refresh_rejectsGarbageToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("not-a-real-token"))))
                .andExpect(status().isUnauthorized());
    }
}
