package za.co.qsnext.employeemanagement.auth;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.ChangePasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.ForgotPasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RefreshTokenRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.auth.dto.ResetPasswordRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.email.Email;
import za.co.qsnext.employeemanagement.email.EmailRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private EmailRepository emailRepository;

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

    @Test
    void login_locksAccount_afterFiveFailedAttempts() throws Exception {
        registerUser("lockout.user", "lockout.user@qsnext.co.za", "S3curePassword!");

        LoginRequest badLogin = new LoginRequest("lockout.user", "WrongPassword!");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        LoginRequest correctLogin = new LoginRequest("lockout.user", "S3curePassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));
    }

    @Test
    void refresh_rejectsReuseOfAnAlreadyRotatedToken() throws Exception {
        String refreshToken = registerAndLogin("rotate.user", "rotate.user@qsnext.co.za", "S3curePassword!");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk());

        /*
         * The original refresh token was rotated (revoked) by the call
         * above. Presenting it again must be rejected rather than silently
         * issuing another token pair.
         */
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        MvcResult loginResult = registerAndLoginRaw(
                "logout.user", "logout.user@qsnext.co.za", "S3curePassword!");

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("whatever"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_allowsLoginWithNewPassword_andRejectsTheOldOne() throws Exception {
        MvcResult loginResult = registerAndLoginRaw(
                "changepw.user", "changepw.user@qsnext.co.za", "S3curePassword!");

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();

        ChangePasswordRequest changeRequest =
                new ChangePasswordRequest("S3curePassword!", "N3wSecurePassword!");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("changepw.user", "S3curePassword!"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("changepw.user", "N3wSecurePassword!"))))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("whatever", "N3wSecurePassword!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_isAlwaysAccepted_evenForAnUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("nobody@qsnext.co.za"))))
                .andExpect(status().isAccepted());
    }

    @Test
    void forgotPasswordAndResetPassword_flowAllowsLoginWithTheNewPassword() throws Exception {
        registerUser("resetpw.user", "resetpw.user@qsnext.co.za", "S3curePassword!");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("resetpw.user@qsnext.co.za"))))
                .andExpect(status().isAccepted());

        List<Email> emails = emailRepository.findAll();

        Email resetEmail = emails.stream()
                .filter(email -> "resetpw.user@qsnext.co.za".equals(email.getRecipient()))
                .filter(email -> "PASSWORD_RESET".equals(email.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No password reset email was queued"));

        String token = extractResetToken(resetEmail.getBody());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(token, "R3setPassword!"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("resetpw.user", "S3curePassword!"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("resetpw.user", "R3setPassword!"))))
                .andExpect(status().isOk());
    }

    @Test
    void logout_revokesTheAccessToken_soItCanNoLongerAuthenticateRequests() throws Exception {
        MvcResult loginResult = registerAndLoginRaw(
                "revoke.access.user", "revoke.access.user@qsnext.co.za", "S3curePassword!");

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();

        // Sanity: the freshly issued access token authenticates successfully.
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_revokesTheAccessTokenUsedToMakeTheRequest() throws Exception {
        MvcResult loginResult = registerAndLoginRaw(
                "revoke.onchange.user", "revoke.onchange.user@qsnext.co.za", "S3curePassword!");

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("S3curePassword!", "N3wSecurePassword!"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockout_immediatelyInvalidatesAnAlreadyIssuedAccessToken() throws Exception {
        MvcResult loginResult = registerAndLoginRaw(
                "cachebust.user", "cachebust.user@qsnext.co.za", "S3curePassword!");

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();

        // Sanity: the token authenticates successfully before the account is locked.
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        LoginRequest badLogin = new LoginRequest("cachebust.user", "WrongPassword!");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        /*
         * The access token itself is still validly signed and unexpired,
         * but the account backing it is now locked. The cached
         * authorization principal must have been evicted so this is
         * enforced immediately rather than for up to the cache TTL.
         */
        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_rejectsAnUnknownToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("not-a-real-token", "N3wSecurePassword!"))))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, password))))
                .andExpect(status().isCreated());
    }

    private MvcResult registerAndLoginRaw(String username, String email, String password) throws Exception {
        registerUser(username, email, password);

        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        MvcResult result = registerAndLoginRaw(username, email, password);

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("refreshToken").asText();
    }

    private String extractResetToken(String emailBody) {
        String marker = "reset your password: ";
        int start = emailBody.indexOf(marker) + marker.length();
        int end = emailBody.indexOf('.', start);
        return emailBody.substring(start, end);
    }
}
