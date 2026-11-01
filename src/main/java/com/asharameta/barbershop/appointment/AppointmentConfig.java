package com.asharameta.barbershop.appointment;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppointmentConfig {
    @Bean
    ToolCallbackProvider appointmentToolsProvider(AppointmentTools appointmentTools) {
        return MethodToolCallbackProvider.builder().toolObjects(appointmentTools).build();
    }
}
