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

@Order(2)
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    @Value("${barbershop.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String headerValue = request.getHeader("B-API-Key");

        if(headerValue != null){
            if(expectedApiKey.equals(headerValue)){
                filterChain.doFilter(request, response);
            }else{
                response.setStatus(HttpStatus.FORBIDDEN.value());
            }
        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }


    }
}
