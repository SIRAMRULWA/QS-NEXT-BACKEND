package za.co.qsnext.employeemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS is opt-in via an explicit origin allowlist rather than a wildcard:
 * the API is called from a separately-hosted single-page frontend, so
 * without this no browser request from that origin would carry an
 * Access-Control-Allow-Origin response header at all. Left empty (the
 * safe default), no cross-origin browser request is allowed.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${security.cors.allowed-origins:}") String allowedOriginsCsv
    ) {
        this.allowedOrigins = allowedOriginsCsv.isBlank()
                ? List.of()
                : Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
