package me.jianwen.mediask.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void publicRequestMatcher_WhenSafeEndpoints_ReturnsTrue() {
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        assertTrue(matches(publicRequestMatcher, HttpMethod.POST, "/api/v1/auth/login"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.POST, "/api/v1/auth/refresh"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/health"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/health/readiness"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/health/liveness"));
    }

    @Test
    void publicRequestMatcher_WhenSensitiveOperationalOrDocsEndpoints_ReturnsFalse() {
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/api/v1/auth/me"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/api/v1/clinic-sessions"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.POST, "/api/v1/registrations"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.POST, "/api/v1/auth/logout"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/info"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/prometheus"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/metrics"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/actuator/metrics/jvm.memory.used"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/v3/api-docs"));
        assertFalse(matches(publicRequestMatcher, HttpMethod.GET, "/swagger-ui/index.html"));
    }

    @Test
    void publicRequestMatcher_WhenPublicDocsEnabled_ReturnsTrueForDocsEndpoints() {
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(true));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/v3/api-docs"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/swagger-ui/index.html"));
        assertTrue(matches(publicRequestMatcher, HttpMethod.GET, "/swagger-ui.html"));
    }

    @Test
    void corsConfigurationSource_WhenApiRequestFromAllowedOrigin_ReturnsCorsRules() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(new ApiCorsProperties(
                List.of("http://localhost:3000", "http://localhost:5173"),
                null,
                null,
                true,
                3600L));

        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/api/v1/auth/me");
        request.setServletPath("/api/v1/auth/me");
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Access-Control-Request-Method", "GET");

        CorsConfiguration configuration = source.getCorsConfiguration(request);
        assertNotNull(configuration);
        assertEquals(List.of("http://localhost:3000", "http://localhost:5173"), configuration.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), configuration.getAllowedMethods());
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
        assertEquals(3600L, configuration.getMaxAge());
    }

    private boolean matches(RequestMatcher publicRequestMatcher, HttpMethod httpMethod, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(httpMethod.name(), requestUri);
        request.setServletPath(requestUri);
        return publicRequestMatcher.matches(request);
    }
}
