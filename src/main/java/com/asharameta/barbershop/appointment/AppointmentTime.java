package com.asharameta.barbershop.appointment;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@AllArgsConstructor
@Component
public class AppointmentTime {
    private final Clock clock;

    public OffsetDateTime toBShopOffsetTime(LocalDateTime time){
        return time.atZone(clock.getZone()).withEarlierOffsetAtOverlap().toOffsetDateTime();
    }

    public boolean isMomentPassed(OffsetDateTime time){
        return clock.instant().isAfter(time.toInstant());
    }
}
