package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String PRIVATE_NETWORKS = "10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16";

    @Test
    void ignoresForwardedFor_whenNoProxiesAreTrusted() {
        ClientIpResolver resolver = new ClientIpResolver("");

        MockHttpServletRequest request = request("10.1.2.3", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("10.1.2.3");
    }

    @Test
    void ignoresForwardedFor_whenPeerIsNotATrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        MockHttpServletRequest request = request("198.51.100.20", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void returnsForwardedClient_whenPeerIsATrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        MockHttpServletRequest request = request("10.1.2.3", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void ignoresCallerSuppliedLeftmostHop_andReturnsRightmostUntrustedHop() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        // The caller sent "X-Forwarded-For: 1.1.1.1" hoping to get a fresh
        // rate-limit bucket; the trusted proxy appended the real address.
        MockHttpServletRequest request = request("10.1.2.3", "1.1.1.1, 203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void skipsEveryTrustedHop_whenMultipleProxiesAppended() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        MockHttpServletRequest request = request("10.1.2.3", "203.0.113.7, 172.16.4.4, 10.9.9.9");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToPeer_whenEveryForwardedHopIsTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        MockHttpServletRequest request = request("10.1.2.3", "192.168.1.1, 10.9.9.9");

        assertThat(resolver.resolve(request)).isEqualTo("10.1.2.3");
    }

    @Test
    void treatsUnparseableHopAsUntrusted() {
        ClientIpResolver resolver = new ClientIpResolver(PRIVATE_NETWORKS);

        MockHttpServletRequest request = request("10.1.2.3", "203.0.113.7, unknown");

        assertThat(resolver.resolve(request)).isEqualTo("unknown");
    }

    private static MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
