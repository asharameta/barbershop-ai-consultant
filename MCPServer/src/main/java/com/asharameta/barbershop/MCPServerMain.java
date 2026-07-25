package com.asharameta.barbershop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.asharameta.barbershop")
public class MCPServerMain {
    public static void main(String[] args) {
        SpringApplication.run(MCPServerMain.class, args);
    }
}