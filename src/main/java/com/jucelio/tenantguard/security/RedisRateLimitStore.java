package com.jucelio.tenantguard.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.security.rate-limit.backend", havingValue = "redis")
public class RedisRateLimitStore implements RateLimitStore {

    private static final DefaultRedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
            "local ttl = redis.call('TTL', KEYS[1]); " +
            "return {current, ttl};",
            List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Bucket consume(String key, long windowSeconds) {
        List<?> result = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of("tenantguard:rate-limit:" + key),
                Long.toString(windowSeconds)
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis did not return a valid rate-limit result.");
        }

        long count = ((Number) result.get(0)).longValue();
        long ttl = Math.max(1, ((Number) result.get(1)).longValue());
        return new Bucket(count, ttl);
    }
}
