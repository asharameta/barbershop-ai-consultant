package com.asharameta.barbershop.service;

import com.asharameta.barbershop.dao.AppointmentDAO;
import com.asharameta.barbershop.model.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Validated
public class BarberService {
    private final AppointmentDAO appointmentDAO;

    @Tool(description = "Book an appointment with a barber by providing barber name, client name, client phone number, optional comment and the date of the appointment")
    public String bookAppointment(
            @ToolParam(description = "barber name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "client name") @NotBlank @Size(max = 100) String clientName,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phoneNumber,
            @ToolParam(description = "optional comment e.g. just a trim") @Size(max = 500) String comment,
            @ToolParam(description = "date and time in ISO format e.g. 2025-04-01T14:00") @NotBlank String dateTime
    ){
        try{
            LocalDateTime parsedDateTime = LocalDateTime.parse(dateTime);
            if (parsedDateTime.isBefore(LocalDateTime.now())) {
                return "Cannot book an appointment in the past: " + dateTime;
            }

            Appointment appointment = Appointment.builder()
                    .barberName(barberName)
                    .clientName(clientName)
                    .phoneNumber(phoneNumber)
                    .dateTime(parsedDateTime)
                    .comment(comment)
                    .build();
            Appointment bookedAppointment = appointmentDAO.bookAppointment(appointment);

            if (bookedAppointment.getStatus() != BookStatus.BOOKED) {
                return "Slot at " + dateTime + " with " + barberName + " is not available.";
            }
            return "Booked! ID: " + bookedAppointment.getId() + " — " + clientName + " with " + barberName + " at " + dateTime;
        } catch (DateTimeParseException e) {
            return "Invalid date/time format: " + dateTime + ". Expected ISO format, e.g. 2025-04-01T14:00.";
        } catch (ConstraintViolationException e) {
            return "Invalid booking details: " + e.getMessage();
        }
    }

    @Tool(description = "Cancel an appointment by client name and phone number")
    public String cancelAppointment(
            @ToolParam(description = "client name") @NotBlank @Size(max = 100) String clientName,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phoneNumber
    ) {
        try{
            boolean isCancelled = appointmentDAO.cancelAppointment(clientName, phoneNumber);
            if (isCancelled) {
                return "Appointment " + clientName + " cancelled successfully.";
            }
            return "Appointment " + clientName + " not found or already cancelled.";
        }catch (ConstraintViolationException e) {
            return "Invalid booking details: " + e.getMessage();
        }
    }

    @Tool(description = "Get all appointments for a specific client using their phone number")
    public String getClientAppointments(
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phoneNumber
    ) {
        List<Appointment> appointments = appointmentDAO.getClientAppointments(phoneNumber);

        if (appointments.isEmpty()) return "No appointments found for this client.";
        return appointments.stream()
                .map(a -> String.format("[%d] %s with %s on %s",
                        a.getId(), a.getClientName(), a.getBarberName(), a.getDateTime()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Return a specific barber schedule using barber name and specific date time")
    public String getBarberSchedule(
            @ToolParam(description = "barber name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "date in format YYYY-MM-DD") @NotBlank String dateTime) {

        try{
            List<Appointment> schedule = appointmentDAO.getBarberSchedule(barberName, dateTime);
            if (schedule.isEmpty()) {
                return String.format("%s has no appointments on %s.", barberName, dateTime);
            }

            return schedule.stream()
                    .map(a -> String.format("  [%d] %s | %s | %s | %s",
                            a.getId(),
                            a.getDateTime().toLocalTime(),
                            a.getClientName(),
                            a.getPhoneNumber(),
                            a.getComment() != null ? a.getComment() : "no comment provided"))
                    .collect(Collectors.joining("\n",
                            String.format("Schedule for %s on %s:\n", barberName, dateTime),
                            ""));

        }catch (DateTimeParseException e) {
            return "Invalid date/time format: " + dateTime + ". Expected ISO format, e.g. 2025-04-01T14:00.";
        } catch (ConstraintViolationException e) {
            return "Invalid booking details: " + e.getMessage();
        }
    }
}
