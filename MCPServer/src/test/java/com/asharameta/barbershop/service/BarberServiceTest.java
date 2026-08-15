package com.asharameta.barbershop.service;

import com.asharameta.barbershop.dao.AppointmentDAO;
import com.asharameta.barbershop.model.Appointment;
import com.asharameta.barbershop.model.BookStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BarberServiceTest {
    @MockitoBean
    AppointmentDAO appointmentDAO;

    @MockitoBean
    BarberService barberService;

    @BeforeEach
    void init() {
        barberService = new BarberService(appointmentDAO);
    }

    @Test
    void testBookAppointmentSuccess() {
        String dateTimeString = "2026-09-09T08:30";
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString);
        Appointment appointment = Appointment.builder()
                .id(13)
                .barberName("Maciej")
                .clientName("Igor")
                .phoneNumber("+48 321 654 987")
                .comment("buzz cut")
                .dateTime(localDateTime)
                .status(BookStatus.BOOKED)
                .build();

        when(appointmentDAO.bookAppointment(any(Appointment.class))).thenReturn(appointment);
        String bookedAppointment = barberService.bookAppointment(appointment.getBarberName(), appointment.getClientName(), appointment.getPhoneNumber(), appointment.getComment(), dateTimeString);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentDAO).bookAppointment(captor.capture());
        assertEquals("Maciej", captor.getValue().getBarberName());
        assertEquals("Igor", captor.getValue().getClientName());
        assertEquals("+48 321 654 987", captor.getValue().getPhoneNumber());
        assertEquals("buzz cut", captor.getValue().getComment());
        assertEquals(localDateTime, captor.getValue().getDateTime());
        assertEquals("Booked! ID: " + appointment.getId() + " — " + appointment.getClientName() + " with " + appointment.getBarberName() + " at " + appointment.getDateTime(), bookedAppointment);
    }

    @Test
    void testProvidedPastDateForBookAppointment() {
        String dateTimeString = "2026-05-15T12:25";
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString);

        String providedPastDateTime = barberService.bookAppointment("Kamil", "Jan", "+48 123 456 789", "", dateTimeString);
        assertEquals("Cannot book an appointment in the past: " + localDateTime, providedPastDateTime);
    }

    @Test
    void testBookIsFailedForBookAppointment() {
        String dateTimeString = "2026-05-15T12:25";
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString);

        String providedPastDateTime = barberService.bookAppointment("Kamil", "Jan", "+48 123 456 789", "", dateTimeString);
        assertEquals("Cannot book an appointment in the past: " + localDateTime, providedPastDateTime);
    }


    @DisplayName("Correctly cancelled appointment")
    @Test
    void testCorrectlyCanceledAppointment() {
        String clientName = "Alex";
        String phoneNumber = "+48 123 456 789";
        when(appointmentDAO.cancelAppointment(clientName,phoneNumber)).thenReturn(true);
        String actualReturnString = barberService.cancelAppointment(clientName, phoneNumber);

        assertEquals( "Appointment " + clientName + " cancelled successfully.", actualReturnString);
    }

    @DisplayName("Cancel incorrect appointment")
    @Test
    void testIncorrectlyCanceledAppointment() {
        String clientName = "John";
        String phoneNumber = "-48 123 456 789";
        when(appointmentDAO.cancelAppointment(clientName,phoneNumber)).thenReturn(false);
        String actualReturnString = barberService.cancelAppointment(clientName, phoneNumber);

        assertEquals( "Appointment " + clientName + " not found or already cancelled.", actualReturnString);
    }

    @DisplayName("Invalid booking details for cancel Appointment")
    @Test
    void testInvalidBookingDetailsProvided() {
//        int id = -1;
//        String phoneNumber = "Some wrong string";
//        String actualReturnString = barberService.cancelAppointment(id, phoneNumber);
//
//        assertTrue(actualReturnString.contains("Invalid booking details"));
    }

    @DisplayName("No appointments found for provided phone number")
    @Test
    void TestNoClientAppointmentsFound() {
        String phoneNumber = "+48 123 456 789";
        when(appointmentDAO.getClientAppointments(phoneNumber)).thenReturn(List.of());
        String noAppointmnetFoundString = barberService.getClientAppointments(phoneNumber);

        assertEquals("No appointments found for this client.", noAppointmnetFoundString);
    }

    @DisplayName("return all appointments found on provided phone number")
    @Test
    void TestGetClientAppointments() {
        String phoneNumber = "+48 123 456 789";
        Appointment appointment1 = Appointment.builder().clientName("Dawid").phoneNumber(phoneNumber).barberName("Alex").build();
        Appointment appointment2 = Appointment.builder().clientName("Dawid").phoneNumber(phoneNumber).barberName("Jan").build();
        Appointment appointment3 = Appointment.builder().clientName("Dawid").phoneNumber(phoneNumber).comment("get rid of gray hair").barberName("Maciej").build();

        List<Appointment> appointments = List.of(appointment1, appointment2, appointment3);

        when(appointmentDAO.getClientAppointments(phoneNumber)).thenReturn(appointments);
        String clientAppointments = barberService.getClientAppointments(phoneNumber);

        String expectedString = appointments.stream()
                .map(a -> String.format("[%d] %s with %s on %s",
                        a.getId(), a.getClientName(), a.getBarberName(), a.getDateTime()))
                .collect(Collectors.joining("\n"));

        assertEquals(expectedString, clientAppointments);
    }

    @Test
    void TestGetEmptyBarberSchedule() {
        String barberName = "Kamil";
        String dateTime = "2026-05-15";
        when(appointmentDAO.getBarberSchedule(barberName, dateTime)).thenReturn(List.of());
        String noAppointmentsFound = barberService.getBarberSchedule(barberName, dateTime);

        assertEquals(String.format("%s has no appointments on %s.", barberName, dateTime), noAppointmentsFound);
    }

    @Test
    void TestGetBarberSchedule() {
        String barberName = "Kamil";
        String dateTimeString = "2026-05-15";
        LocalDateTime parsedDateTime = LocalDateTime.of(2026, Month.MAY, 15, 7, 0);
        Appointment appointment1 = Appointment.builder().clientName("Dawid").barberName(barberName).dateTime(parsedDateTime).build();
        Appointment appointment2 = Appointment.builder().clientName("Maciej").barberName(barberName).dateTime(parsedDateTime.plusHours(1)).build();
        Appointment appointment3 = Appointment.builder().clientName("Mariusz").barberName(barberName).dateTime(parsedDateTime.plusHours(4)).build();
        Appointment appointment4 = Appointment.builder().clientName("Marek").barberName(barberName).dateTime(parsedDateTime.plusHours(6)).build();

        List<Appointment> appointments = List.of(appointment1, appointment2, appointment3, appointment4);

        when(appointmentDAO.getBarberSchedule(barberName, dateTimeString)).thenReturn(appointments);
        String barberSchedule = barberService.getBarberSchedule(barberName, dateTimeString);

        String expectedString = appointments.stream()
                .map(a -> String.format("  [%d] %s | %s | %s | %s",
                        a.getId(),
                        a.getDateTime().toLocalTime(),
                        a.getClientName(),
                        a.getPhoneNumber(),
                        a.getComment() != null ? a.getComment() : "no comment provided"))
                .collect(Collectors.joining("\n",
                        String.format("Schedule for %s on %s:\n", barberName, dateTimeString),
                        ""));

        assertEquals(expectedString, barberSchedule);
    }

    @Test
    void TestInvalidDateTimeFormatPassedToGetBarberSchedule() {
        String barberName = "Kamil";
        String dateTime = "15.08.2026";
        DateTimeParseException dateTimeParseException = new DateTimeParseException("irrelevant", "bad-input", 0);
        when(appointmentDAO.getBarberSchedule(barberName, dateTime)).thenThrow(dateTimeParseException);

        String returnedString = barberService.getBarberSchedule(barberName, dateTime);

        assertEquals("Invalid date/time format: " + dateTime + ". Expected ISO format, e.g. 2025-04-01T14:00.", returnedString);
    }
}