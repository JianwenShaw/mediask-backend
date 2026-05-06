package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.dto.CurrentUserResponse;
import me.jianwen.mediask.api.dto.LoginRequest;
import me.jianwen.mediask.api.dto.LoginResponse;
import me.jianwen.mediask.api.dto.LogoutRequest;
import me.jianwen.mediask.api.dto.RefreshTokenRequest;
import me.jianwen.mediask.api.dto.RefreshTokenResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.user.command.LogoutCommand;
import me.jianwen.mediask.application.user.command.LoginCommand;
import me.jianwen.mediask.application.user.command.RefreshTokenCommand;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.application.user.usecase.GetCurrentUserUseCase;
import me.jianwen.mediask.application.user.usecase.LoginUseCase;
import me.jianwen.mediask.application.user.usecase.LogoutUseCase;
import me.jianwen.mediask.application.user.usecase.RefreshTokenUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AuditApiSupport auditApiSupport;

    public AuthController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase,
            AuditApiSupport auditApiSupport) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        var auditContext = auditApiSupport.currentContext(null, request.phone());
        try {
            var result = loginUseCase.handle(new LoginCommand(request.phone(), request.password()), auditContext);
            LoginResponse response = AuthAssembler.toLoginResponse(result);
            return Result.ok(response);
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    auditContext,
                    AuditActionCodes.AUTH_LOGIN_FAILED,
                    AuditResourceTypes.AUTH_SESSION,
                    null,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public Result<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = AuthAssembler.toRefreshTokenResponse(
                refreshTokenUseCase.handle(new RefreshTokenCommand(request.refreshToken())));
        return Result.ok(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestBody LogoutRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String accessToken = resolveAccessToken(authorizationHeader);
        if (accessToken == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        logoutUseCase.handle(new LogoutCommand(
                request.refreshToken(),
                accessToken,
                principal.userId()), auditApiSupport.currentContext(principal));
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        CurrentUserResponse response = AuthAssembler.toCurrentUserResponse(
                getCurrentUserUseCase.handle(new GetCurrentUserQuery(principal.userId())));
        return Result.ok(response);
    }

    private String resolveAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String normalized = authorizationHeader.trim();
        if (!normalized.startsWith("Bearer ")) {
            return null;
        }
        String token = normalized.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
