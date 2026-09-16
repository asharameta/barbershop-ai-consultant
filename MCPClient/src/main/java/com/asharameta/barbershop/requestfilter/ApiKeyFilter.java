package com.asharameta.barbershop.requestfilter;

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

@Order(2)
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "esk_";
    private final String expectedApiKey;

    public ApiKeyFilter(@Value("${barbershop.api-key}") String apiKey)
    {
        expectedApiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String headerValue = request.getHeader("B-API-Key");

        if(headerValue != null && headerValue.startsWith(BEARER_PREFIX)){
            if(expectedApiKey.equals(headerValue)){
                filterChain.doFilter(request, response);
            }else{
                response.setStatus(HttpStatus.FORBIDDEN.value());
            }
        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
