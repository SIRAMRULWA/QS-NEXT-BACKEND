package za.co.qsnext.employeemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Backs {@code @Async} work (publishing outbox events to RabbitMQ off the
 * request thread) and {@code @Scheduled} jobs (the outbox sweep).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("qsnext-async-");
        executor.initialize();

        return executor;
    }
}
