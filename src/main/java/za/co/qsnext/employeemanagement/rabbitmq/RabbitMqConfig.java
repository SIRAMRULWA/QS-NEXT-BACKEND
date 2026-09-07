package za.co.qsnext.employeemanagement.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    /*
     * Main email exchange.
     */
    @Bean
    public DirectExchange emailExchange() {

        return new DirectExchange(
                RabbitMqConstants.EMAIL_EXCHANGE
        );
    }

    /*
     * Dead Letter Exchange.
     */
    @Bean
    public DirectExchange emailDeadLetterExchange() {

        return new DirectExchange(
                RabbitMqConstants.EMAIL_DLX
        );
    }

    /*
     * Main email queue.
     *
     * Messages that cannot be processed successfully
     * will eventually be routed to the Dead Letter Exchange.
     */
    @Bean
    public Queue emailQueue() {

        return QueueBuilder
                .durable(
                        RabbitMqConstants.EMAIL_QUEUE
                )
                .deadLetterExchange(
                        RabbitMqConstants.EMAIL_DLX
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.EMAIL_DLQ_ROUTING_KEY
                )
                .build();
    }

    /*
     * Dead Letter Queue.
     */
    @Bean
    public Queue emailDeadLetterQueue() {

        return QueueBuilder
                .durable(
                        RabbitMqConstants.EMAIL_DLQ
                )
                .build();
    }

    /*
     * Main exchange → email queue.
     */
    @Bean
    public Binding emailQueueBinding(
            Queue emailQueue,
            DirectExchange emailExchange
    ) {

        return BindingBuilder
                .bind(emailQueue)
                .to(emailExchange)
                .with(
                        RabbitMqConstants.EMAIL_ROUTING_KEY
                );
    }

    /*
     * Dead Letter Exchange → Dead Letter Queue.
     */
    @Bean
    public Binding emailDeadLetterQueueBinding(
            Queue emailDeadLetterQueue,
            DirectExchange emailDeadLetterExchange
    ) {

        return BindingBuilder
                .bind(emailDeadLetterQueue)
                .to(emailDeadLetterExchange)
                .with(
                        RabbitMqConstants.EMAIL_DLQ_ROUTING_KEY
                );
    }

    /*
     * RabbitTemplate is used by the application
     * to publish messages to RabbitMQ.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory
    ) {

        return new RabbitTemplate(
                connectionFactory
        );
    }
}