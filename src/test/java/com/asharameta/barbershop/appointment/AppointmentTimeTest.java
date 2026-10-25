package com.asharameta.barbershop.appointment;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentTimeTest {
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private final Clock fixed = Clock.fixed(Instant.parse("2032-06-01T10:00:00Z"), WARSAW);
    private final AppointmentTime appointmentTime = new AppointmentTime(fixed);

    @Test
    public void summerTimeGivesPlusTwoOffset(){
        var localDateTime = LocalDateTime.of(2032, 6, 1, 14, 0);

        var offset = appointmentTime.toBShopOffsetTime(localDateTime);

        assertEquals(ZoneOffset.ofHours(2), offset.getOffset());
        assertEquals(14, offset.getHour());
    }

    @Test
    public void winterTimeGivesPlusOneOffset(){
        var localDateTime = LocalDateTime.of(2026, 10, 25, 3, 0);

        var offset = appointmentTime.toBShopOffsetTime(localDateTime);

        assertEquals(ZoneOffset.ofHours(1), offset.getOffset());
        assertEquals(3, offset.getHour());
    }

    @Test
    public void timePassedTest(){
        var fixedLocalDateTime = LocalDateTime.of(2032, 6, 1, 12, 0);
        var localDateTimeAfterFixed = LocalDateTime.of(2032, 6, 1, 14, 0);
        var localDateTimeBeforeFixed = LocalDateTime.of(2032, 6, 1, 8, 0);

        var fixedOffset = appointmentTime.toBShopOffsetTime(fixedLocalDateTime);
        var offsetAfter = appointmentTime.toBShopOffsetTime(localDateTimeAfterFixed);
        var offsetBefore = appointmentTime.toBShopOffsetTime(localDateTimeBeforeFixed);

        assertFalse(appointmentTime.isMomentPassed(fixedOffset));
        assertFalse(appointmentTime.isMomentPassed(offsetAfter));
        assertTrue(appointmentTime.isMomentPassed(offsetBefore));
    }

    @Test
    public void timeInDaylightSavingGapIsShiftedForward(){
        var localWrongDateTime = LocalDateTime.of(2032,3,28,2,30);
        var wrongOffset = appointmentTime.toBShopOffsetTime(localWrongDateTime);

        assertTrue(appointmentTime.isMomentPassed(wrongOffset));
    }
}
