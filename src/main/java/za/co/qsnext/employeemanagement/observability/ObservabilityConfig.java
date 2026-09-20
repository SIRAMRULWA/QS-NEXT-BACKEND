package za.co.qsnext.employeemanagement.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ObservabilityConfig {

    /**
     * Explicitly ordered ahead of everything else, including Spring
     * Security's own filter chain (which Spring Boot registers at a
     * low-but-not-minimum order) - a correlation ID is only useful if
     * it covers the whole request, including a 401/403 Security itself
     * rejects before any controller runs.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {

        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");

        return registration;
    }
}
