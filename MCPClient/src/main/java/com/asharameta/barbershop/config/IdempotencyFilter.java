package com.asharameta.barbershop.config;

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

@Order(3)
@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    @Value("${idempotency.max-body-bytes}")
    private long maxBodyBytes;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(request.getContentLengthLong() > maxBodyBytes){
            response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        }else{
            filterChain.doFilter(request, response);
        }
    }
}
