package za.co.qsnext.employeemanagement.common;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;

/**
 * Base class for repository-layer tests. Runs against the real
 * Testcontainers-provided PostgreSQL instance (not an in-memory database)
 * and with Flyway applying the real migrations, so unique/foreign-key
 * constraints and native queries behave exactly as they would in
 * production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractRepositoryTest {
}
