package za.co.qsnext.employeemanagement.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;

/**
 * Base class for full-stack integration tests: real Spring context, real
 * security filter chain and a real PostgreSQL/RabbitMQ/Redis instance
 * provided by Testcontainers. Extend this for controller and workflow
 * tests that need to exercise authentication/authorization end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
