package za.co.qsnext.employeemanagement.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailMessagePublisherTest {

    @Autowired
    private EmailMessagePublisher publisher;

    @Test
    void shouldPublishEmailEvent() {

        EmailEvent event = new EmailEvent(
                "employee@example.com",
                "RabbitMQ Test",
                "RabbitMQ is working successfully."
        );

        publisher.publish(event);
    }
}