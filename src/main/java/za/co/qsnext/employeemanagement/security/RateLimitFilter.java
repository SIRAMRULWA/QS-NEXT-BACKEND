package za.co.qsnext.employeemanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import za.co.qsnext.employeemanagement.exception.ErrorResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Fixed-window per-IP rate limiting for the authentication endpoints that
 * can be probed without being authenticated first (login, register,
 * forgot/reset password). Anything behind authentication already has
 * account-lockout (login) or requires a valid token, so this is
 * deliberately scoped narrowly rather than applied globally.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification"
    );

    private static final String KEY_PREFIX = "ratelimit:";
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;
    private final int maxRequestsPerWindow;
    private final Duration window;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ClientIpResolver clientIpResolver,
            @Value("${security.rate-limit.max-requests}") int maxRequestsPerWindow,
            @Value("${security.rate-limit.window-seconds}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clientIpResolver = clientIpResolver;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String key = KEY_PREFIX + request.getRequestURI() + ":" + clientIpResolver.resolve(request);

        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            redisTemplate.expire(key, window);
        }

        if (requestCount != null && requestCount > maxRequestsPerWindow) {

            ErrorResponse errorResponse = new ErrorResponse(
                    OffsetDateTime.now(),
                    SC_TOO_MANY_REQUESTS,
                    "TOO_MANY_REQUESTS",
                    "Too many requests. Please try again later.",
                    request.getRequestURI()
            );

            response.setStatus(SC_TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), errorResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
