package com.asharameta.barbershop.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Service
public class IdempotencyService {
    private static final String KEY_PREFIX = "idem:chat:";
    private static final String STATE_PROCESSING = "processing";
    private static final String STATE_DONE = "done";

    private final StringRedisTemplate redis;
    private final long lockTtlSeconds;
    private final long resultTtlMinutes;

    public IdempotencyService(StringRedisTemplate redis,
                              @Value("${idempotency.lock-ttl-seconds}") long lockTtlSeconds,
                              @Value("${idempotency.result-ttl-minutes}") long resultTtlMinutes
    ) {
        this.redis = redis;
        this.lockTtlSeconds = lockTtlSeconds;
        this.resultTtlMinutes = resultTtlMinutes;
    }

    public IdempotencyDTO claim(String requestBody, String idempotencyKey){
        String bodyHash = HashUtil.sha256(requestBody);
        String redisKey = KEY_PREFIX + idempotencyKey;

        Boolean claimed = redis.opsForValue()
                .setIfAbsent(redisKey, STATE_PROCESSING + ":" + bodyHash, Duration.ofSeconds(lockTtlSeconds));

        if(Boolean.TRUE.equals(claimed)){
            return new IdempotencyDTO(IdempotencyState.CLAIMED, null, null);
        }

        String existing = redis.opsForValue().get(redisKey);

        if (existing == null) {
            return new IdempotencyDTO(IdempotencyState.LOCK_EXPIRED, HttpStatus.CONFLICT.value(),
                    "Please retry, request expired mid-flight");
        }

        String[] parts = existing.split(":", 3);
        String state = parts[0];
        String storedHash = parts[1];

        if (!storedHash.equals(bodyHash)) {
            return new IdempotencyDTO(IdempotencyState.KEY_REUSED, HttpStatus.UNPROCESSABLE_CONTENT.value(),
                    "Idempotency key reused with a different request body");
        }
        if (STATE_PROCESSING.equals(state)) {
            return new IdempotencyDTO(IdempotencyState.DUPLICATE_IN_PROGRESS, HttpStatus.CONFLICT.value(),
                    "Request already in progress");
        }

        return new IdempotencyDTO(IdempotencyState.COMPLETED, HttpStatus.OK.value(), parts[2]);
    }

    public void complete(String idempotencyKey, String requestBody, String responseBody){
        String bodyHash = HashUtil.sha256(requestBody);
        String redisKey = KEY_PREFIX + idempotencyKey;
        redis.opsForValue().set(redisKey, STATE_DONE + ":" + bodyHash + ":" + responseBody, Duration.ofMinutes(resultTtlMinutes));
    }

    public void release(String idempotencyKey){
        redis.delete(KEY_PREFIX + idempotencyKey);
    }
}
