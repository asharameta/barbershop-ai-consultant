package com.asharameta.barbershop.appointment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class AppointmentTime {
    private final Clock clock;
    private final LocalTime opening;
    private final LocalTime closing;

    public AppointmentTime(Clock clock,
                           @Value("${appointment.opening-time}") String opening,
                           @Value("${appointment.closing-time}") String closing) {
        this.clock = clock;
        this.opening = LocalTime.parse(opening);
        this.closing = LocalTime.parse(closing);
    }

    public OffsetDateTime toBShopOffsetTime(LocalDateTime time){
        return time.atZone(clock.getZone()).withEarlierOffsetAtOverlap().toOffsetDateTime();
    }

    public boolean isMomentPassed(OffsetDateTime time){
        return clock.instant().isAfter(time.toInstant());
    }

    public boolean isWithinWorkingHours(OffsetDateTime start, Duration slot){
        ZonedDateTime from = start.atZoneSameInstant(clock.getZone());
        ZonedDateTime to = from.plus(slot);
        return !from.toLocalTime().isBefore(opening)
                && from.toLocalDate().equals(to.toLocalDate())
                && !to.toLocalTime().isAfter(closing);
    }

    public String workingHours(){
        return opening + "-" + closing;
    }

    public LocalDateTime toShopLocal(OffsetDateTime time){
        return time.atZoneSameInstant(clock.getZone()).toLocalDateTime();
    }
}
