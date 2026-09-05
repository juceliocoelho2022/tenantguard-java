package com.jucelio.tenantguard;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "tenantguard-test-secret-key-that-is-long-enough-for-hs256-signing";

    private final JwtService jwtService = new JwtService(SECRET, 3600, 604800);
    private final AuthenticatedUser user = new AuthenticatedUser("user-a", "TENANT_A", "USER");

    @Test
    void shouldGenerateAccessTokenWithTenantAndRole() {
        String token = jwtService.generateAccessToken(user);
        var claims = jwtService.parseAccessToken(token);

        assertEquals("user-a", claims.getSubject());
        assertEquals("TENANT_A", claims.get("tenant_id", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(JwtService.ACCESS_TOKEN_TYPE,
                claims.get(JwtService.TOKEN_TYPE_CLAIM, String.class));
    }

    @Test
    void shouldGenerateAndParseRefreshToken() {
        String token = jwtService.generateRefreshToken(user);
        AuthenticatedUser refreshedUser = jwtService.parseRefreshToken(token);

        assertEquals(user.username(), refreshedUser.username());
        assertEquals(user.tenantId(), refreshedUser.tenantId());
        assertEquals(user.role(), refreshedUser.role());
    }

    @Test
    void refreshTokenMustNotBeAcceptedAsAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThrows(IllegalArgumentException.class,
                () -> jwtService.parseAccessToken(refreshToken));
    }

    @Test
    void accessTokenMustNotBeAcceptedAsRefreshToken() {
        String accessToken = jwtService.generateAccessToken(user);

        assertThrows(IllegalArgumentException.class,
                () -> jwtService.parseRefreshToken(accessToken));
    }
}
