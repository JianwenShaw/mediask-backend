package me.jianwen.mediask.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.slf4j.MDC;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenCodec accessTokenCodec;
    private final AccessTokenBlocklistPort accessTokenBlocklistPort;
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonSecurityFailureResponder securityFailureResponder;
    private final RequestMatcher publicRequestMatcher;

    public JwtAuthenticationFilter(
            AccessTokenCodec accessTokenCodec,
            AccessTokenBlocklistPort accessTokenBlocklistPort,
            UserAuthenticationRepository userAuthenticationRepository,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            ObjectMapper objectMapper,
            RequestMatcher publicRequestMatcher) {
        this.accessTokenCodec = accessTokenCodec;
        this.accessTokenBlocklistPort = accessTokenBlocklistPort;
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.securityFailureResponder = new JsonSecurityFailureResponder(objectMapper);
        this.publicRequestMatcher = publicRequestMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String previousUserId = MDC.get(RequestConstants.MDC_USER_ID);
        boolean publicRequest = publicRequestMatcher.matches(request);
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            MDC.remove(RequestConstants.MDC_USER_ID);
            try {
                filterChain.doFilter(request, response);
            } finally {
                restoreUserId(previousUserId);
            }
            return;
        }

        try {
            AccessTokenClaims accessTokenClaims = parseAccessToken(authorizationHeader);
            establishAuthentication(request, accessTokenClaims);
            filterChain.doFilter(request, response);
        } catch (BizException exception) {
            SecurityContextHolder.clearContext();
            request.removeAttribute(RequestConstants.USER_ID_ATTRIBUTE);
            if (publicRequest) {
                MDC.remove(RequestConstants.MDC_USER_ID);
                filterChain.doFilter(request, response);
                return;
            }
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("invalid access token", exception));
        } catch (AuthenticationProcessingException exception) {
            SecurityContextHolder.clearContext();
            request.removeAttribute(RequestConstants.USER_ID_ATTRIBUTE);
            securityFailureResponder.writeInternalError(request, response, ErrorCode.SYSTEM_ERROR);
        } finally {
            restoreUserId(previousUserId);
        }
    }

    private void establishAuthentication(HttpServletRequest request, AccessTokenClaims accessTokenClaims) {
        try {
            bindAuthenticatedUser(request, accessTokenClaims);
        } catch (BizException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthenticationProcessingException(exception);
        }
    }

    private AccessTokenClaims parseAccessToken(String authorizationHeader) {
        try {
            AccessTokenClaims accessTokenClaims = accessTokenCodec.parseAccessToken(
                    authorizationHeader.substring(BEARER_PREFIX.length()).trim());
            if (accessTokenBlocklistPort.isBlocked(accessTokenClaims.tokenId())) {
                throw new InsufficientAuthenticationException("access token revoked");
            }
            return accessTokenClaims;
        } catch (BizException | InsufficientAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthenticationProcessingException(exception);
        }
    }

    private void bindAuthenticatedUser(HttpServletRequest request, AccessTokenClaims accessTokenClaims) {
        AuthenticatedUser authenticatedUser = userAuthenticationRepository
                .findAuthenticatedUserById(accessTokenClaims.userId())
                .orElseThrow(() -> new BizException(UserErrorCode.AUTHENTICATED_USER_NOT_FOUND));
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

    private static final class AuthenticationProcessingException extends RuntimeException {

        private AuthenticationProcessingException(Throwable cause) {
            super(cause);
        }
    }
}
