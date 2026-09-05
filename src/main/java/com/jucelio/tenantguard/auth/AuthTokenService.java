package com.jucelio.tenantguard.auth;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AuthTokenService {

    private final JwtService jwtService;
    private final RefreshTokenSessionRepository repository;
    private final RlsTenantGuard rlsTenantGuard;

    public AuthTokenService(
            JwtService jwtService,
            RefreshTokenSessionRepository repository,
            RlsTenantGuard rlsTenantGuard) {
        this.jwtService = jwtService;
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
    }

    @Transactional
    public LoginResponse issueTokens(AuthenticatedUser user) {
        rlsTenantGuard.applyTenant(user.tenantId());
        return issueNewSession(user);
    }

    @Transactional
    public LoginResponse rotate(String refreshToken) {
        JwtService.RefreshTokenDetails details = jwtService.parseRefreshTokenDetails(refreshToken);
        rlsTenantGuard.applyTenant(details.user().tenantId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshTokenSession session = repository.findByJtiForUpdate(details.jti())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token não reconhecido."));

        validateSession(session, details, now);

        session.revoke(now);
        repository.save(session);

        return issueNewSession(details.user());
    }

    @Transactional
    public void revoke(String refreshToken) {
        JwtService.RefreshTokenDetails details = jwtService.parseRefreshTokenDetails(refreshToken);
        rlsTenantGuard.applyTenant(details.user().tenantId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshTokenSession session = repository.findByJtiForUpdate(details.jti())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token não reconhecido."));

        validateSession(session, details, now);
        session.revoke(now);
        repository.save(session);
    }

    private LoginResponse issueNewSession(AuthenticatedUser user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user, jti);
        JwtService.RefreshTokenDetails details = jwtService.parseRefreshTokenDetails(refreshToken);

        repository.save(new RefreshTokenSession(
                jti,
                user.username(),
                user.tenantId(),
                user.role(),
                details.expiresAt().atOffset(ZoneOffset.UTC),
                now
        ));

        return new LoginResponse(
                jwtService.generateAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.accessExpirationSeconds(),
                user.tenantId()
        );
    }

    private void validateSession(
            RefreshTokenSession session,
            JwtService.RefreshTokenDetails details,
            OffsetDateTime now) {

        AuthenticatedUser user = details.user();

        if (!session.isActive(now)
                || !session.getUsername().equals(user.username())
                || !session.getTenantId().equals(user.tenantId())
                || !session.getRole().equals(user.role())) {
            throw new IllegalArgumentException("Refresh token revogado, expirado ou inconsistente.");
        }
    }
}
