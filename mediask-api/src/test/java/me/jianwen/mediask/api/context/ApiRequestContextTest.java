package me.jianwen.mediask.api.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.request.RequestContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ApiRequestContextTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void currentRequestContext_WhenSecurityPrincipalAvailable_DoesNotDependOnRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test/patient-self");
        request.setAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE, "req-test-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                2003L,
                "patient_li",
                "Patient Li",
                "PATIENT",
                List.of("PATIENT"),
                List.of("patient:profile:view:self"),
                List.of(),
                2201L,
                null,
                null,
                List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));

        RequestContextSnapshot snapshot = ApiRequestContext.currentRequestContext();

        assertEquals("req-test-1", snapshot.requestId());
        assertEquals("/api/v1/test/patient-self", snapshot.requestUri());
        assertEquals("2003", snapshot.userId());
    }
}
