package com.barbershopAIConsultant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = "com.barbershopAIConsultant")
public class MCPClientMain {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MCPClientMain.class, args);
        System.out.println("Hello, MCPClientMain World!");
    }
}