package com.barbershopAIConsultant.Service;

import com.barbershopAIConsultant.Model.Appointment;
import com.barbershopAIConsultant.Model.AppointmentDAO;
import com.barbershopAIConsultant.Model.BookStatus;
import lombok.AllArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BarberService {
    private final AppointmentDAO appointmentDAO;

    @Tool(description = "Say hello to a person by name. Returns a greeting message.")
    public String sayHello(
            @ToolParam(description = "The name of the person to greet") String name) {
        return "Hello, " + name + "!";
    }

    @Tool(description = "Book an appointment with a barber")
    public String bookAppointment(
            @ToolParam(description = "barber name") String barberName,
            @ToolParam(description = "client name") String clientName,
            @ToolParam(description = "client phone number") String phoneNumber,
            @ToolParam(description = "optional comment e.g. just a trim") String comment,
            @ToolParam(description = "date and time in ISO format e.g. 2025-04-01T14:00") String dateTime
    ){
        Appointment appointment = Appointment.builder()
                                            .barberName(barberName)
                                            .clientName(clientName)
                                            .phoneNumber(phoneNumber)
                                            .dateTime(LocalDateTime.parse(dateTime))
                                            .comment(comment)
                                            .build();
        Appointment bookedAppointment = appointmentDAO.bookAppointment(appointment);
        if (bookedAppointment.getStatus() != BookStatus.BOOKED) {
            return "Slot at " + dateTime + " with " + barberName + " is not available.";
        }
        return "Booked! ID: " + bookedAppointment.getId() + " — " + clientName + " with " + barberName + " at " + dateTime;
    }

    @Tool(description = "Cancel an appointment by its ID")
    public String cancelAppointment(
            @ToolParam(description = "appointment ID to cancel") int id
    ) {
        boolean cancelled = appointmentDAO.cancelAppointment(id);
        if (cancelled) {
            return "Appointment " + id + " cancelled successfully.";
        }
        return "Appointment " + id + " not found or already cancelled.";
    }

    @Tool(description = "Get all appointments for a specific client")
    public String getClientAppointments(
            @ToolParam(description = "client phone number") String phoneNumber
    ) {
        List<Appointment> appointments = appointmentDAO.getClientAppointments(phoneNumber);


        if (appointments.isEmpty()) return "No appointments found for this client.";
        return appointments.stream()
                .map(a -> String.format("[%d] %s with %s on %s",
                        a.getId(), a.getClientName(), a.getBarberName(), a.getDateTime()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Return a specific barber schedule")
    public String getBarberSchedule(
            @ToolParam(description = "barber name") String barberName,
            @ToolParam(description = "date in format YYYY-MM-DD") String date){
        List<Appointment> schedule = appointmentDAO.getBarberSchedule(barberName, date);
        if (schedule.isEmpty()) {
            return String.format("%s has no appointments on %s.", barberName, date);
        }

        return schedule.stream()
                .map(a -> String.format("  [%d] %s | %s | %s | %s",
                        a.getId(),
                        a.getDateTime().toLocalTime(),
                        a.getClientName(),
                        a.getPhoneNumber(),
                        a.getComment() != null ? a.getComment() : "no comment"))
                .collect(Collectors.joining("\n",
                        String.format("Schedule for %s on %s:\n", barberName, date),
                        ""));
    }
}
