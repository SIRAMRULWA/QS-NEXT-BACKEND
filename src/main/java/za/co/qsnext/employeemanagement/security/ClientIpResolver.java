package za.co.qsnext.employeemanagement.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the real client IP for rate limiting and audit logging.
 * {@code X-Forwarded-For} is client-supplied and untrustworthy by default -
 * blindly trusting it (the previous behaviour, duplicated in three places)
 * let an external caller spoof a fresh rate-limit bucket on every request
 * simply by sending a different header value, defeating the login/register
 * brute-force protection entirely, and let attacker-controlled values end
 * up recorded as the "client IP" in the audit trail and refresh-token
 * metadata.
 *
 * <p>The header is only honoured when the request's own TCP peer
 * ({@link HttpServletRequest#getRemoteAddr()}) matches a configured,
 * trusted reverse proxy/load balancer. With no trusted proxies configured
 * (the default), every request is attributed to its direct peer address
 * regardless of any forwarding header it presents - the same safe default
 * a naive deployment without a reverse proxy needs, and one an operator
 * deploying behind a real load balancer opts out of explicitly by listing
 * its address(es).
 */
@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final List<IpAddressMatcher> trustedProxyMatchers;

    public ClientIpResolver(
            @Value("${security.trusted-proxies:}") String trustedProxiesCsv
    ) {
        this.trustedProxyMatchers = trustedProxiesCsv.isBlank()
                ? List.of()
                : List.of(trustedProxiesCsv.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(proxy -> !proxy.isBlank())
                        .map(IpAddressMatcher::new)
                        .toList();
    }

    public String resolve(HttpServletRequest request) {

        if (request == null) {
            return null;
        }

        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {

            String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {

        if (remoteAddr == null || trustedProxyMatchers.isEmpty()) {
            return false;
        }

        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddr));
    }
}
