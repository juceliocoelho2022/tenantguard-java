package com.jucelio.tenantguard.security;

public interface RateLimitStore {

    Bucket consume(String key, long windowSeconds);

    record Bucket(long count, long retryAfterSeconds) {}
}
