package za.co.qsnext.employeemanagement.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gives every request a correlation ID - the caller's own {@code
 * X-Correlation-Id} header if it sent one, otherwise a freshly
 * generated one - and makes it available two ways: in the MDC, so
 * every log line written while handling this request carries it (see
 * logback-spring.xml's pattern), and echoed back on the response so a
 * client can quote it when reporting an issue. {@link AuditService}
 * also reads it from the MDC to stamp audit records with it.
 * <p>
 * Registered outside Spring Security's filter chain (see {@link
 * ObservabilityConfig}) and ordered to run first, so it covers every
 * request including ones Security itself rejects (401/403) - those are
 * exactly the requests most worth being able to trace.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER_NAME);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // The servlet container reuses this thread for other
            // requests - never let one request's correlation ID leak
            // into the next.
            MDC.remove(MDC_KEY);
        }
    }
}
