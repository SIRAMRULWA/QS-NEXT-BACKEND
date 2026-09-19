package za.co.qsnext.employeemanagement.common;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;
import za.co.qsnext.employeemanagement.email.StubEmailSender;
import za.co.qsnext.employeemanagement.email.TestEmailConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/**
 * Base class for full-stack integration tests: real Spring context, real
 * security filter chain and a real PostgreSQL/RabbitMQ/Redis instance
 * provided by Testcontainers. Extend this for controller and workflow
 * tests that need to exercise authentication/authorization end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestEmailConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StubEmailSender stubEmailSender;

    @BeforeEach
    void resetStubEmailSender() {
        stubEmailSender.reset();
    }

    /**
     * Polls a condition until it is true or the timeout elapses. The
     * outbox -> RabbitMQ -> consumer pipeline is asynchronous by design,
     * so tests that assert on its effects (an email was sent, a record
     * transitioned to a terminal status) cannot check immediately after
     * the triggering HTTP call returns.
     */
    protected void awaitUntil(BooleanSupplier condition, Duration timeout) {

        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {

            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        throw new AssertionError("Condition was not met within " + timeout);
    }
}
