package com.apchavez.customers.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    private MockServerWebExchange buildPostExchange(String ip) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/api/v1/customers")
                .remoteAddress(new InetSocketAddress(ip, 80))
                .build();
        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange buildGetExchange(String ip) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/v1/customers/active")
                .remoteAddress(new InetSocketAddress(ip, 80))
                .build();
        return MockServerWebExchange.from(request);
    }

    private WebFilterChain passThroughChain() {
        return exchange -> Mono.empty();
    }

    @Test
    void should_allow_get_requests_without_counting() {
        MockServerWebExchange exchange = buildGetExchange("1.1.1.1");

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void should_allow_post_request_within_limit() {
        MockServerWebExchange exchange = buildPostExchange("2.2.2.2");

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void should_block_post_request_when_limit_exceeded() {
        String ip = "3.3.3.3";
        WebFilterChain chain = passThroughChain();

        // Exhaust the 100-request limit
        for (int i = 0; i < 100; i++) {
            filter.filter(buildPostExchange(ip), chain).block();
        }

        // 101st request must be blocked
        MockServerWebExchange blocked = buildPostExchange(ip);
        filter.filter(blocked, chain).block();

        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getResponse().getHeaders().getFirst("Retry-After")).isNotNull();
    }

    @Test
    void should_track_limits_independently_per_ip() {
        String ip1 = "4.4.4.4";
        String ip2 = "5.5.5.5";
        WebFilterChain chain = passThroughChain();

        // Exhaust IP1
        for (int i = 0; i < 100; i++) {
            filter.filter(buildPostExchange(ip1), chain).block();
        }
        MockServerWebExchange blockedIp1 = buildPostExchange(ip1);
        filter.filter(blockedIp1, chain).block();
        assertThat(blockedIp1.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // IP2 should still be allowed
        MockServerWebExchange allowedIp2 = buildPostExchange(ip2);
        filter.filter(allowedIp2, chain).block();
        assertThat(allowedIp2.getResponse().getStatusCode()).isNull();
    }

    @Test
    void should_use_last_ip_from_x_forwarded_for_header() {
        String spoofedIp = "9.9.9.9";
        String realIp = "10.10.10.10";

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/api/v1/customers")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                .header("X-Forwarded-For", spoofedIp + ", " + realIp)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = passThroughChain();

        // Exhaust 100 requests using the spoofed IP (should not affect real IP)
        for (int i = 0; i < 100; i++) {
            MockServerHttpRequest req = MockServerHttpRequest
                    .method(HttpMethod.POST, "/api/v1/customers")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                    .header("X-Forwarded-For", spoofedIp + ", " + realIp)
                    .build();
            filter.filter(MockServerWebExchange.from(req), chain).block();
        }

        // 101st should be blocked (same X-Forwarded-For last IP = realIp)
        MockServerWebExchange blocked = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/customers")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                        .header("X-Forwarded-For", spoofedIp + ", " + realIp)
                        .build());
        filter.filter(blocked, chain).block();
        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
