package za.co.qsnext.employeemanagement.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public EmailMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(EmailEvent event) {

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EMAIL_EXCHANGE,
                RabbitMqConstants.EMAIL_ROUTING_KEY,
                event
        );
    }
}