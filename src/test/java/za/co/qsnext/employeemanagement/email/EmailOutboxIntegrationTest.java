package za.co.qsnext.employeemanagement.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;

import za.co.qsnext.employeemanagement.audit.AuditLogRepository;
import za.co.qsnext.employeemanagement.auth.dto.ForgotPasswordRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the transactional-outbox pipeline: an Email row is
 * inserted by a business action, published to RabbitMQ after the
 * transaction commits, consumed and handed to the (stubbed) sender, and
 * either marked SENT or - after retries are exhausted - dead-lettered.
 * The stub sender replaces real SMTP (see AbstractIntegrationTest /
 * TestEmailConfiguration) so this is deterministic without a mail server.
 */
class EmailOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void forgotPassword_eventuallyDeliversTheQueuedEmail() throws Exception {
        String email = "outbox.success@qsnext.co.za";

        registerUser("outbox.success", email);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isAccepted());

        awaitUntil(
                () -> stubEmailSender.countSentTo(email) >= 1,
                Duration.ofSeconds(10)
        );

        Optional<Email> passwordResetEmail = emailRepository.findAll().stream()
                .filter(e -> email.equals(e.getRecipient()))
                .filter(e -> "PASSWORD_RESET".equals(e.getType()))
                .findFirst();

        assertThat(passwordResetEmail).isPresent();

        // A longer window than the first await: the single-threaded email
        // consumer processes this whole suite's messages serially, and the
        // WELCOME email counted by the first await says nothing about how
        // far behind the PASSWORD_RESET email queued after it is.
        awaitUntil(
                () -> emailRepository.findById(passwordResetEmail.get().getId())
                        .map(e -> e.getStatus() == EmailStatus.SENT)
                        .orElse(false),
                Duration.ofSeconds(20)
        );
    }

    @Test
    void register_eventuallyDeliversAWelcomeEmail() throws Exception {
        String email = "outbox.welcome@qsnext.co.za";

        registerUser("outbox.welcome", email);

        awaitUntil(
                () -> stubEmailSender.countSentTo(email) >= 1,
                Duration.ofSeconds(10)
        );
    }

    @Test
    void deliveryFailure_exhaustsRetriesAndDeadLettersTheEmail() throws Exception {
        String email = "outbox.failure@qsnext.co.za";

        registerUser("outbox.failure", email);

        // The WELCOME email from registration is allowed to succeed; only
        // fail deliveries queued from this point on (the PASSWORD_RESET
        // email below).
        stubEmailSender.alwaysFailDeliveryTo(email);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isAccepted());

        awaitUntil(
                () -> emailRepository.findAll().stream()
                        .filter(e -> email.equals(e.getRecipient()))
                        .filter(e -> "PASSWORD_RESET".equals(e.getType()))
                        .anyMatch(e -> e.getStatus() == EmailStatus.FAILED
                                && e.getAttemptCount() > 0),
                Duration.ofSeconds(15)
        );

        Email failedEmail = emailRepository.findAll().stream()
                .filter(e -> email.equals(e.getRecipient()))
                .filter(e -> "PASSWORD_RESET".equals(e.getType()))
                .findFirst()
                .orElseThrow();

        awaitUntil(
                () -> auditLogRepository
                        .findByAction("EMAIL_DEAD_LETTERED", PageRequest.of(0, 50, Sort.by("createdAt").descending()))
                        .stream()
                        .anyMatch(log -> failedEmail.getId().equals(log.getEntityId())),
                Duration.ofSeconds(15)
        );
    }

    private void registerUser(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, "S3curePassword!"))))
                .andExpect(status().isCreated());
    }
}
