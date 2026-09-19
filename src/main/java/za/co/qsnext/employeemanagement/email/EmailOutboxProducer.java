package za.co.qsnext.employeemanagement.email;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import za.co.qsnext.employeemanagement.rabbitmq.RabbitMqConstants;

import java.util.UUID;

@Component
public class EmailOutboxProducer {

    private final RabbitTemplate rabbitTemplate;

    public EmailOutboxProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(UUID emailId) {
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EMAIL_EXCHANGE,
                RabbitMqConstants.EMAIL_ROUTING_KEY,
                new EmailMessage(emailId)
        );
    }
}
