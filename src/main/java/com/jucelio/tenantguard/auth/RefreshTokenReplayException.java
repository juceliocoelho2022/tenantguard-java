package com.jucelio.tenantguard.auth;

import com.jucelio.tenantguard.security.AuthenticatedUser;

public class RefreshTokenReplayException extends RuntimeException {

    private final AuthenticatedUser user;

    public RefreshTokenReplayException(AuthenticatedUser user) {
        super("Refresh token replay detected.");
        this.user = user;
    }

    public AuthenticatedUser getUser() {
        return user;
    }
}
