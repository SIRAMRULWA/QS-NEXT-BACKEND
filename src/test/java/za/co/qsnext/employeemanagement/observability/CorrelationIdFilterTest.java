package za.co.qsnext.employeemanagement.observability;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void doFilterInternal_generatesAndPropagatesACorrelationId_whenTheCallerSendsNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seenDuringChain = new AtomicReference<>();
        FilterChain capturing = (req, res) -> seenDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, capturing);

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(responseHeader).isNotBlank();
        assertThat(seenDuringChain.get()).isEqualTo(responseHeader);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_propagatesTheCallersOwnCorrelationId() throws Exception {
        String callerCorrelationId = UUID.randomUUID().toString();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, callerCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seenDuringChain = new AtomicReference<>();
        FilterChain capturing = (req, res) -> seenDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, capturing);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(callerCorrelationId);
        assertThat(seenDuringChain.get()).isEqualTo(callerCorrelationId);
    }

    @Test
    void doFilterInternal_clearsMdc_evenWhenTheChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain throwingChain = (req, res) -> {
            throw new java.io.IOException("simulated downstream failure");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.doFilter(request, response, throwingChain))
                .isInstanceOf(java.io.IOException.class);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
