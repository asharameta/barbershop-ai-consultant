package com.asharameta.barbershop.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Order(1)
@Component
public class RateLimiter extends OncePerRequestFilter {
    @Value("${ratelimit.capacity}")
    private int maxLimit;
    @Value("${ratelimit-refill-per-minute}")
    private int refillTokensPerMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String identity = request.getRemoteAddr();

        Bucket bucket = buckets.computeIfAbsent(identity, b->newBucket());

        if(bucket.tryConsume(1)){
            filterChain.doFilter(request, response);
        }else{
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    private Bucket newBucket(){
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxLimit)
                .refillGreedy(refillTokensPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
