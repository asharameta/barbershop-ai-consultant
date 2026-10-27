package com.asharameta.barbershop.appointment;

import com.asharameta.barbershop.appointment.AppointmentDAO.BusyTime;
import com.asharameta.barbershop.appointment.AppointmentDAO.ClientAppointment;
import com.asharameta.barbershop.appointment.BookingResult.BarberNotFound;
import com.asharameta.barbershop.appointment.BookingResult.Booked;
import com.asharameta.barbershop.appointment.BookingResult.SlotTaken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@Validated
class AppointmentService {
    static final String SHOP_ID = "barbershopId";

    private static final String PHONE = "^\\+?[0-9]{7,15}$";
    private static final String BAD_FORMAT = "Invalid date/time format: %s. Expected ISO format, e.g. 2025-04-01T14:00.";

    private final AppointmentDAO appointmentDAO;
    private final AppointmentTime appointmentTime;
    private final Duration slot;

    AppointmentService(AppointmentDAO appointmentDAO, AppointmentTime appointmentTime,
                       @Value("${appointment.slot-duration}") long slotMinutes) {
        this.appointmentDAO = appointmentDAO;
        this.appointmentTime = appointmentTime;
        this.slot = Duration.ofMinutes(slotMinutes);
    }

    @Tool(description = "Book an appointment with a barber of this barbershop. Needs the barber's first name and surname, client name, client phone number, optional comment and the date and time of the appointment")
    public String bookAppointment(
            @ToolParam(description = "barber first name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "barber surname") @NotBlank @Size(max = 100) String barberSurname,
            @ToolParam(description = "client name") @NotBlank @Size(max = 100) String clientName,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            @ToolParam(description = "optional comment e.g. just a trim", required = false) @Size(max = 500) String comment,
            @ToolParam(description = "date and time in ISO format e.g. 2025-04-01T14:00") @NotBlank String dateTime,
            ToolContext toolContext
    ) {
        OffsetDateTime start;
        try {
            start = appointmentTime.toBShopOffsetTime(LocalDateTime.parse(dateTime));
        } catch (DateTimeParseException e) {
            return BAD_FORMAT.formatted(dateTime);
        }
        String problem = slotProblem(start, dateTime);
        if (problem != null) return problem;

        return switch (appointmentDAO.book(shopId(toolContext), barberName, barberSurname, clientName,
                phoneNumber, comment, start, start.plus(slot))) {
            case Booked b -> "Booked! ID: %d — %s with %s %s on %s".formatted(
                    b.appointment().getId(), clientName, barberName, barberSurname, dateTime);
            case SlotTaken s -> "Slot at %s with %s %s is not available.".formatted(dateTime, barberName, barberSurname);
            case BarberNotFound n -> "No barber named %s %s works in this barbershop.".formatted(barberName, barberSurname);
        };
    }

    @Tool(description = "Move an existing appointment to another date and time, identified by appointment id and the client's phone number")
    public String rescheduleAppointment(
            @ToolParam(description = "appointment id") long appointmentId,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            @ToolParam(description = "new date and time in ISO format e.g. 2025-04-01T14:00") @NotBlank String dateTime,
            ToolContext toolContext
    ) {
        OffsetDateTime start;
        try {
            start = appointmentTime.toBShopOffsetTime(LocalDateTime.parse(dateTime));
        } catch (DateTimeParseException e) {
            return BAD_FORMAT.formatted(dateTime);
        }
        String problem = slotProblem(start, dateTime);
        if (problem != null) return problem;

        return appointmentDAO.reschedule(shopId(toolContext), appointmentId, phoneNumber, start, start.plus(slot))
                .map(result -> switch (result) {
                    case Booked b -> "Appointment %d rescheduled to %s.".formatted(appointmentId, dateTime);
                    case SlotTaken s -> "Slot at %s is not available.".formatted(dateTime);
                    case BarberNotFound n -> throw new IllegalStateException("reschedule does not look up a barber");
                })
                .orElse("No upcoming booked appointment with this id for this phone number.");
    }

    @Tool(description = "Cancel an appointment, identified by appointment id and the client's phone number")
    public String cancelAppointment(
            @ToolParam(description = "appointment id") long appointmentId,
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            ToolContext toolContext
    ) {
        return appointmentDAO.cancel(shopId(toolContext), appointmentId, phoneNumber)
                .map(a -> "Appointment %d cancelled.".formatted(a.getId()))
                .orElse("No upcoming booked appointment with this id for this phone number.");
    }

    @Tool(description = "List the upcoming appointments of a client using their phone number")
    public String getClientAppointments(
            @ToolParam(description = "client phone number") @NotBlank @Pattern(regexp = PHONE) String phoneNumber,
            ToolContext toolContext
    ) {
        List<ClientAppointment> appointments = appointmentDAO.clientAppointments(shopId(toolContext), phoneNumber);
        if (appointments.isEmpty()) return "No upcoming appointments found for this client.";
        return appointments.stream()
                .map(a -> "[%d] with %s on %s".formatted(a.id(), a.barber(), appointmentTime.toShopLocal(a.startsAt())))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Show the times a barber is already booked on a given day, so free times can be suggested")
    public String getBusyTimes(
            @ToolParam(description = "barber first name") @NotBlank @Size(max = 100) String barberName,
            @ToolParam(description = "barber surname") @NotBlank @Size(max = 100) String barberSurname,
            @ToolParam(description = "date in format YYYY-MM-DD") @NotBlank String date,
            ToolContext toolContext
    ) {
        LocalDate day;
        try {
            day = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return "Invalid date format: " + date + ". Expected YYYY-MM-DD.";
        }
        List<BusyTime> busy = appointmentDAO.busyTimes(shopId(toolContext), barberName, barberSurname,
                appointmentTime.toBShopOffsetTime(day.atStartOfDay()),
                appointmentTime.toBShopOffsetTime(day.plusDays(1).atStartOfDay()));
        String who = barberName + " " + barberSurname;
        if (busy.isEmpty()) {
            return "%s has no appointments on %s (working hours %s).".formatted(who, date, appointmentTime.workingHours());
        }
        return busy.stream()
                .map(b -> appointmentTime.toShopLocal(b.startsAt()).toLocalTime() + "-" + appointmentTime.toShopLocal(b.endsAt()).toLocalTime())
                .collect(Collectors.joining(", ", "%s is booked on %s at: ".formatted(who, date), " (working hours " + appointmentTime.workingHours() + ")"));
    }

    private String slotProblem(OffsetDateTime start, String dateTime) {
        if (appointmentTime.isMomentPassed(start)) {
            return "Cannot book an appointment in the past: " + dateTime;
        }
        if (!appointmentTime.isWithinWorkingHours(start, slot)) {
            return "%s is outside working hours (%s); an appointment takes %d minutes.".formatted(
                    dateTime, appointmentTime.workingHours(), slot.toMinutes());
        }
        return null;
    }

    private static long shopId(ToolContext toolContext) {
        return (Long) Objects.requireNonNull(toolContext.getContext().get(SHOP_ID), "barbershopId missing from tool context");
    }
}
