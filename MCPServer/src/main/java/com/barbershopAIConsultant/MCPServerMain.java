package com.barbershopAIConsultant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.barbershopAIConsultant")
public class MCPServerMain {
    public static void main(String[] args) {
        SpringApplication.run(MCPServerMain.class, args);
        System.out.println("Hello, MCPServerMain World!");
    }
}