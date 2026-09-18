package com.asharameta.barbershop.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Order(1)
@Component
class RateLimiter extends OncePerRequestFilter {
    private final RateLimiterService rateLimiterService;

    RateLimiter(RateLimiterService rateLimiterService){
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = resolveKey(request);
        ConsumptionProbe probe = rateLimiterService.tryConsume(identity);

        response.setHeader("B-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if(probe.isConsumed()){
            filterChain.doFilter(request, response);
        }else{
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
        }
    }


    private String resolveKey(HttpServletRequest request){
        String apiKey = request.getHeader("B-API-Key");
        return apiKey != null ? apiKey : request.getRemoteAddr();
    }
}
