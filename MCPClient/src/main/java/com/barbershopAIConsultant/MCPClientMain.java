package com.barbershopAIConsultant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.barbershopAIConsultant")
public class MCPClientMain {
    public static void main(String[] args) {
        SpringApplication.run(MCPClientMain.class, args);
        System.out.println("Hello, MCPClientMain World!");
    }
}