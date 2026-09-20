package za.co.qsnext.employeemanagement.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    private final int consumerMaxAttempts;
    private final long consumerInitialIntervalMs;
    private final double consumerBackoffMultiplier;
    private final long consumerMaxIntervalMs;
    private final int consumerConcurrency;
    private final int consumerMaxConcurrency;

    public RabbitMqConfig(
            @Value("${email.consumer.max-attempts}") int consumerMaxAttempts,
            @Value("${email.consumer.initial-interval-ms}") long consumerInitialIntervalMs,
            @Value("${email.consumer.backoff-multiplier}") double consumerBackoffMultiplier,
            @Value("${email.consumer.max-interval-ms}") long consumerMaxIntervalMs,
            @Value("${email.consumer.concurrency:1}") int consumerConcurrency,
            @Value("${email.consumer.max-concurrency:1}") int consumerMaxConcurrency
    ) {
        this.consumerMaxAttempts = consumerMaxAttempts;
        this.consumerInitialIntervalMs = consumerInitialIntervalMs;
        this.consumerBackoffMultiplier = consumerBackoffMultiplier;
        this.consumerMaxIntervalMs = consumerMaxIntervalMs;
        this.consumerConcurrency = consumerConcurrency;
        this.consumerMaxConcurrency = consumerMaxConcurrency;
    }

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
                .durable(
                        RabbitMqConstants.EMAIL_QUEUE
                )
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
                .durable(
                        RabbitMqConstants.EMAIL_DEAD_LETTER_QUEUE
                )
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
                .with(
                        RabbitMqConstants.EMAIL_ROUTING_KEY
                );
    }

    @Bean
    public Binding emailDeadLetterQueueBinding(
            Queue emailDeadLetterQueue,
            DirectExchange emailDeadLetterExchange
    ) {

        return BindingBuilder
                .bind(emailDeadLetterQueue)
                .to(emailDeadLetterExchange)
                .with(
                        RabbitMqConstants.EMAIL_DEAD_LETTER_ROUTING_KEY
                );
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper().findAndRegisterModules());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * Retries a failed delivery in-process with exponential backoff before
     * giving up. {@link RejectAndDontRequeueRecoverer} rejects the message
     * without requeueing once attempts are exhausted, which - given the
     * queue's dead-letter-exchange configuration above - is exactly what
     * routes it to the DLQ.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(consumerConcurrency);
        factory.setMaxConcurrentConsumers(consumerMaxConcurrency);

        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                // maxRetries is retries *after* the first attempt, so
                // subtract one to make consumerMaxAttempts mean the total
                // number of tries (matching the property's name).
                .maxRetries(Math.max(consumerMaxAttempts - 1, 0))
                .backOffOptions(
                        consumerInitialIntervalMs,
                        consumerBackoffMultiplier,
                        consumerMaxIntervalMs
                )
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();

        factory.setAdviceChain(retryAdvice);

        return factory;
    }
}
