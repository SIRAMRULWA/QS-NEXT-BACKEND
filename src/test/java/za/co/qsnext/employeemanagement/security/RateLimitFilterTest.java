package za.co.qsnext.employeemanagement.security;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private static final int MAX_REQUESTS = 3;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();

        filter = new RateLimitFilter(
                redisTemplate,
                objectMapper,
                new ClientIpResolver(""),
                MAX_REQUESTS,
                60L
        );
    }

    @Test
    void allowsRequest_toANonRateLimitedPath_withoutTouchingRedis() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void allowsRequest_whenUnderTheLimit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequest_onceOverTheLimit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_REQUESTS + 1);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void tracksDifferentClientIps_separately() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("ratelimit:/api/v1/auth/login:10.0.0.1")).thenReturn(1L);
        when(valueOperations.increment("ratelimit:/api/v1/auth/login:10.0.0.2")).thenReturn(1L);

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        firstRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        secondRequest.setRemoteAddr("10.0.0.2");

        filter.doFilter(firstRequest, new MockHttpServletResponse(), filterChain);
        filter.doFilter(secondRequest, new MockHttpServletResponse(), filterChain);

        verify(valueOperations).increment("ratelimit:/api/v1/auth/login:10.0.0.1");
        verify(valueOperations).increment("ratelimit:/api/v1/auth/login:10.0.0.2");
    }
}
