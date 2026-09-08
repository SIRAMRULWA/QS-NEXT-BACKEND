package za.co.qsnext.employeemanagement.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(
                RabbitMqConstants.EMAIL_EXCHANGE
        );
    }

    @Bean
    public DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(
                RabbitMqConstants.EMAIL_DEAD_LETTER_EXCHANGE
        );
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.EMAIL_QUEUE)
                .deadLetterExchange(
                        RabbitMqConstants.EMAIL_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.EMAIL_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.EMAIL_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Binding emailQueueBinding(
            Queue emailQueue,
            DirectExchange emailExchange
    ) {
        return BindingBuilder
                .bind(emailQueue)
                .to(emailExchange)
                .with(RabbitMqConstants.EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDeadLetterQueueBinding(
            Queue emailDeadLetterQueue,
            DirectExchange emailDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(emailDeadLetterQueue)
                .to(emailDeadLetterExchange)
                .with(RabbitMqConstants.EMAIL_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter rabbitJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter rabbitJsonMessageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(
                rabbitJsonMessageConverter
        );

        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory
    ) {
        return new RabbitAdmin(connectionFactory);
    }
}