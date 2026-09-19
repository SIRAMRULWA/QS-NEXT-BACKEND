package za.co.qsnext.employeemanagement;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against the Testcontainers-provided
 * PostgreSQL, RabbitMQ and Redis (via {@code mvn spring-boot:test-run}),
 * so a developer doesn't need to stand up that infrastructure by hand.
 */
public class TestQsnextEmployeeManagementSystemApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");

        SpringApplication
                .from(QsnextEmployeeManagementSystemApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
