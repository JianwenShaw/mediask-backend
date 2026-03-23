package me.jianwen.mediask.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import org.slf4j.MDC;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenCodec accessTokenCodec;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final RequestMatcher publicRequestMatcher;

    public JwtAuthenticationFilter(
            AccessTokenCodec accessTokenCodec,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            RequestMatcher publicRequestMatcher) {
        this.accessTokenCodec = accessTokenCodec;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.publicRequestMatcher = publicRequestMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (publicRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = accessTokenCodec.parseAccessToken(
                    authorizationHeader.substring(BEARER_PREFIX.length()).trim());
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            request.removeAttribute(RequestConstants.USER_ID_ATTRIBUTE);
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("invalid access token", exception));
            return;
        }

        String previousUserId = MDC.get(RequestConstants.MDC_USER_ID);
        try {
            bindAuthenticatedUser(request, authenticatedUser);
            filterChain.doFilter(request, response);
        } finally {
            restoreUserId(previousUserId);
        }
    }

    private void bindAuthenticatedUser(HttpServletRequest request, AuthenticatedUser authenticatedUser) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(authenticatedUser);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String userId = String.valueOf(authenticatedUser.userId());
        request.setAttribute(RequestConstants.USER_ID_ATTRIBUTE, userId);
        MDC.put(RequestConstants.MDC_USER_ID, userId);
    }

    private void restoreUserId(String previousUserId) {
        if (previousUserId == null || previousUserId.isBlank()) {
            MDC.remove(RequestConstants.MDC_USER_ID);
            return;
        }
        MDC.put(RequestConstants.MDC_USER_ID, previousUserId);
    }
}
