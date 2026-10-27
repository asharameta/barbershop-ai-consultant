package com.asharameta.barbershop.appointment;

import com.asharameta.barbershop.appointment.BookingResult.BarberNotFound;
import com.asharameta.barbershop.appointment.BookingResult.Booked;
import com.asharameta.barbershop.appointment.BookingResult.SlotTaken;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;

import java.time.*;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final ToolContext CONTEXT = new ToolContext(Map.of(AppointmentService.SHOP_ID, 7L));

    private final Clock fixed = Clock.fixed(Instant.parse("2032-06-01T10:00:00Z"), WARSAW);
    private final AppointmentDAO appointmentDAO = mock(AppointmentDAO.class);
    private final AppointmentService service =
            new AppointmentService(appointmentDAO, new AppointmentTime(fixed, "10:00", "20:00"), 45);

    private String book(String dateTime) {
        return service.bookAppointment("Tomasz", "Nowicki", "Jan", "+48500100200", null, dateTime, CONTEXT);
    }

    @Test
    void booksWithEndAfterOneSlotAndTheShopFromContext() {
        when(appointmentDAO.book(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new Booked(Appointment.builder().id(42L).build()));

        String reply = book("2032-06-01T14:00");

        var start = ArgumentCaptor.forClass(OffsetDateTime.class);
        var end = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(appointmentDAO).book(eq(7L), eq("Tomasz"), eq("Nowicki"), eq("Jan"), eq("+48500100200"),
                eq(null), start.capture(), end.capture());
        assertEquals(OffsetDateTime.parse("2032-06-01T14:00+02:00"), start.getValue());
        assertEquals(OffsetDateTime.parse("2032-06-01T14:45+02:00"), end.getValue());
        assertTrue(reply.startsWith("Booked! ID: 42"));
    }

    @Test
    void reportsTakenSlot() {
        when(appointmentDAO.book(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new SlotTaken());

        assertTrue(book("2032-06-01T14:00").contains("not available"));
    }

    @Test
    void reportsUnknownBarber() {
        when(appointmentDAO.book(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new BarberNotFound());

        assertTrue(book("2032-06-01T14:00").contains("No barber named Tomasz Nowicki"));
    }

    @Test
    void rejectsPastTimeWithoutTouchingTheDatabase() {
        assertTrue(book("2032-06-01T11:00").contains("in the past"));

        verify(appointmentDAO, never()).book(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsOutsideWorkingHoursWithoutTouchingTheDatabase() {
        assertTrue(book("2032-06-01T21:00").contains("outside working hours"));

        verify(appointmentDAO, never()).book(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsGarbageDateTime() {
        assertTrue(book("tomorrow at 3").contains("Invalid date/time format"));
    }

    @Test
    void rescheduleReportsMissingAppointment() {
        when(appointmentDAO.reschedule(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(Optional.empty());

        String reply = service.rescheduleAppointment(5, "+48500100200", "2032-06-01T15:00", CONTEXT);

        assertTrue(reply.startsWith("No upcoming booked appointment"));
    }
}
