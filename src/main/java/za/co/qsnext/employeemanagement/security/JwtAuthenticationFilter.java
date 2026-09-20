package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserAuthorizationCacheService userAuthorizationCacheService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserAuthorizationCacheService userAuthorizationCacheService,
            TokenRevocationService tokenRevocationService
    ) {
        this.jwtService = jwtService;
        this.userAuthorizationCacheService = userAuthorizationCacheService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        /*
         * No JWT provided.
         *
         * Continue the request.
         * Spring Security will determine whether
         * the endpoint requires authentication.
         */
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        );

        try {

            String username = jwtService.extractUsername(token);

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                /*
                 * Only access tokens may authenticate protected API
                 * requests, and only while still signed, unexpired and
                 * not individually revoked (logout, password change,
                 * detected refresh-token reuse).
                 */
                if (jwtService.isAccessToken(token)
                        && jwtService.isTokenValid(token, username)) {

                    UUID tokenId = jwtService.extractTokenId(token);

                    if (!tokenRevocationService.isRevoked(tokenId)) {

                        userAuthorizationCacheService
                                .findPrincipal(username)
                                .map(CustomUserDetails::new)
                                .filter(CustomUserDetails::isEnabled)
                                .filter(CustomUserDetails::isAccountNonLocked)
                                .ifPresent(userDetails -> {

                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails,
                                                    null,
                                                    userDetails.getAuthorities()
                                            );

                                    authentication.setDetails(
                                            new WebAuthenticationDetailsSource()
                                                    .buildDetails(request)
                                    );

                                    SecurityContextHolder
                                            .getContext()
                                            .setAuthentication(authentication);
                                });
                    }
                }
            }

        } catch (JwtException | IllegalArgumentException ex) {

            /*
             * Invalid, expired or malformed JWT.
             *
             * Do not authenticate the request.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
