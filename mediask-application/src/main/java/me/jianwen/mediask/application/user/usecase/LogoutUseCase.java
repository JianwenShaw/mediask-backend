package me.jianwen.mediask.application.user.usecase;

import java.time.Clock;
import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.command.LogoutCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import org.springframework.transaction.annotation.Transactional;

public class LogoutUseCase {

    private final RefreshTokenStore refreshTokenStore;
    private final AccessTokenBlocklistPort accessTokenBlocklistPort;
    private final AccessTokenCodec accessTokenCodec;
    private final Clock clock;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public LogoutUseCase(
            RefreshTokenStore refreshTokenStore,
            AccessTokenBlocklistPort accessTokenBlocklistPort,
            AccessTokenCodec accessTokenCodec,
            Clock clock,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.refreshTokenStore = refreshTokenStore;
        this.accessTokenBlocklistPort = accessTokenBlocklistPort;
        this.accessTokenCodec = accessTokenCodec;
        this.clock = clock;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void handle(LogoutCommand command, AuditContext auditContext) {
        RefreshTokenSession refreshTokenSession = refreshTokenStore.findByTokenValue(command.refreshToken())
                .orElseThrow(() -> new BizException(UserErrorCode.INVALID_REFRESH_TOKEN));
        if (refreshTokenSession.isExpiredAt(clock.instant())) {
            refreshTokenStore.deleteByTokenValue(command.refreshToken());
            throw new BizException(UserErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if (command.authenticatedUserId() != null
                && !refreshTokenSession.userId().equals(command.authenticatedUserId())) {
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }

        AccessTokenClaims accessTokenClaims = parseOptionalAccessToken(command.accessToken());
        if (accessTokenClaims != null && !refreshTokenSession.userId().equals(accessTokenClaims.userId())) {
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }
        if (accessTokenClaims != null && accessTokenClaims.sessionId() == null) {
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }
        if (accessTokenClaims != null
                && !refreshTokenSession.tokenId().equals(accessTokenClaims.sessionId())) {
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }

        if (accessTokenClaims != null) {
            accessTokenBlocklistPort.block(accessTokenClaims.tokenId(), accessTokenClaims.expiresAt());
        }
        refreshTokenStore.deleteByTokenValue(command.refreshToken());
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.AUTH_LOGOUT,
                AuditResourceTypes.AUTH_SESSION,
                command.authenticatedUserId() == null ? null : String.valueOf(command.authenticatedUserId()),
                null,
                null,
                null);
    }

    private AccessTokenClaims parseOptionalAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return accessTokenCodec.parseAccessToken(accessToken);
        } catch (BizException exception) {
            return null;
        }
    }
}
