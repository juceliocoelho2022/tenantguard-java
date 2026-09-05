package com.jucelio.tenantguard.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitStoreTest {

    @Test
    void shouldReturnCounterAndTtlFromRedisScript() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), eq(List.of("tenantguard:rate-limit:login:127.0.0.1")), eq("60")))
                .thenReturn(List.of(3L, 42L));

        RedisRateLimitStore store = new RedisRateLimitStore(redisTemplate);

        RateLimitStore.Bucket bucket = store.consume("login:127.0.0.1", 60);

        assertEquals(3, bucket.count());
        assertEquals(42, bucket.retryAfterSeconds());
        verify(redisTemplate).execute(any(), eq(List.of("tenantguard:rate-limit:login:127.0.0.1")), eq("60"));
    }
}
