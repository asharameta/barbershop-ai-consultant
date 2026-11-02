package com.asharameta.barbershop.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class AppointmentTools {
    private static final String PHONE = "^\\+?[0-9]{7,15}$";

    private final AppointmentService appointmentService;

    @Tool(description = "Book an appointment with a barber of this barbershop. Needs the barber's first name and surname, client name, client phone number, optional comment and the date and time of the appointment")
    String bookAppointment(
            @ToolParam(description = "barber first name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "barber surname") @NotBlank @Size(max = 100) String barberSurname,
            @ToolParam(description = "client name") @NotBlank @Size(max = 100) String clientName,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            @ToolParam(description = "optional comment e.g. just a trim", required = false) @Size(max = 500) String comment,
            @ToolParam(description = "date and time in ISO format e.g. 2025-04-01T14:00") @NotBlank String dateTime,

            ToolContext toolContext
    ) {
        return appointmentService.bookAppointment(barberName,barberSurname,clientName,phoneNumber,comment,dateTime, toolContext);
    }

    @Tool(description = "Move an existing appointment to another date and time, identified by appointment id and the client's phone number")
    String rescheduleAppointment(
            @ToolParam(description = "appointment id") long appointmentId,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            @ToolParam(description = "new date and time in ISO format e.g. 2025-04-01T14:00") @NotBlank String dateTime,
            ToolContext toolContext
    ) {
        return appointmentService.rescheduleAppointment(appointmentId, phoneNumber, dateTime, toolContext);
    }

    @Tool(description = "Cancel an appointment, identified by appointment id and the client's phone number")
    String cancelAppointment(
            @ToolParam(description = "appointment id") long appointmentId,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            ToolContext toolContext
    ) {
        return appointmentService.cancelAppointment(appointmentId,phoneNumber,toolContext);
    }

    @Tool(description = "List the upcoming appointments of a client using their phone number")
    String getClientAppointments(
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            ToolContext toolContext
    ) {
        return appointmentService.getClientAppointments(phoneNumber, toolContext);
    }

    @Tool(description = "Show the times a barber is already booked on a given day, so free times can be suggested")
    String getBusyTimes(
            @ToolParam(description = "barber first name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "barber surname") @NotBlank @Size(max = 100) String barberSurname,
            @ToolParam(description = "date in format YYYY-MM-DD") @NotBlank String date,
            ToolContext toolContext
    ) {
        return appointmentService.getBusyTimes(barberName, barberSurname, date, toolContext);
    }
}
