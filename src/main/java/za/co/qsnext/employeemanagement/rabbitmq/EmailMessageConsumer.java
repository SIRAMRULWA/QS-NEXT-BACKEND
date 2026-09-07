package za.co.qsnext.employeemanagement.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EmailMessageConsumer.class);

    @RabbitListener(
            queues = RabbitMqConstants.EMAIL_QUEUE
    )
    public void consumeEmailMessage(EmailEvent event) {

        log.info(
                "Received email event for recipient: {}",
                event.recipient()
        );

        log.info(
                "Email subject: {}",
                event.subject()
        );

        log.debug(
                "Email body: {}",
                event.body()
        );
    }
}