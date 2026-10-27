package com.asharameta.barbershop.appointment;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppointmentConfig {
    // lives here because AppointmentService is package-private
    @Bean
    ToolCallbackProvider appointmentTools(AppointmentService appointmentService) {
        return MethodToolCallbackProvider.builder().toolObjects(appointmentService).build();
    }
}
