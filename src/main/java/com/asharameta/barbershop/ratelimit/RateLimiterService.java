package com.asharameta.barbershop.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
class RateLimiterService {
    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfig;
    private final int TOKEN_AMOUNT_TO_CONSUME = 1;

    RateLimiterService(ProxyManager<String> proxyManager,
                              @Value("${ratelimit.capacity}") int maxLimit,
                              @Value("${ratelimit-refill-per-minute}") int refillTokensPerMinute) {
        this.proxyManager = proxyManager;

        bucketConfig = () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(maxLimit).refillGreedy(refillTokensPerMinute, Duration.ofMinutes(1))).build();
    }

    ConsumptionProbe tryConsume(String key){
        Bucket bucket = proxyManager.getProxy(key, bucketConfig);
        return bucket.tryConsumeAndReturnRemaining(TOKEN_AMOUNT_TO_CONSUME);
    }
}
