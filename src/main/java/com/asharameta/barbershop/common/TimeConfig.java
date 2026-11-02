package com.asharameta.barbershop.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    public Clock clock(@Value("${barbershop.timezone}") String timeZone){
        return Clock.system(ZoneId.of(timeZone));
    }
}
