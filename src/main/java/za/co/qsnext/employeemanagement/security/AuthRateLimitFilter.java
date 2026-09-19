package za.co.qsnext.employeemanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import za.co.qsnext.employeemanagement.exception.ErrorResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Limits POST attempts against the authentication endpoints per client IP
 * to slow down credential-stuffing and brute-force attempts.
 *
 * Deliberately not a {@code @Component}: it is constructed and wired
 * directly into the security filter chain in {@code SecurityConfig} so it
 * is never also auto-registered by Spring Boot as a blanket servlet
 * filter running on every request.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    private static final String KEY_PREFIX = "auth-rate-limit:";
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!HttpMethod.POST.matches(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI())) {

            filterChain.doFilter(request, response);
            return;
        }

        String key = KEY_PREFIX
                + clientIp(request)
                + ":"
                + request.getRequestURI();

        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            writeTooManyRequests(response, request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            String path
    ) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                "Too many attempts. Please try again later.",
                path
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String clientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
