package com.asharameta.barbershop.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
class RateLimiterConfig {

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, byte[]> bucket4jConnection(LettuceConnectionFactory redisConnectionFactory) {
        if (!(redisConnectionFactory.getNativeClient() instanceof RedisClient redisClient)) {
            throw new IllegalStateException("Expected standalone RedisClient, got: " + redisConnectionFactory.getNativeClient());
        }
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> bucket4jConnection){
        return Bucket4jLettuce.casBasedBuilder(bucket4jConnection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(2)))
                .build();
    }
}
