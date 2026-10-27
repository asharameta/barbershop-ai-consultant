package com.asharameta.barbershop.appointment;

import com.asharameta.barbershop.appointment.BookingResult.*;
import lombok.AllArgsConstructor;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
class AppointmentDAO {
    private static final String OVERLAP_CONSTRAINT = "no_overlapping_appointments";
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final JdbcTemplate jdbcTemplate;

    record ClientAppointment(long id, String barber, OffsetDateTime startsAt) {}

    record BusyTime(OffsetDateTime startsAt, OffsetDateTime endsAt) {}

    BookingResult book(long barbershopId, String barberName, String barberSurname, String clientName,
                       String phoneNumber, String comment, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        String sql = """
            INSERT INTO appointment (barber_id, client_name, client_phone_number, comment, starts_at, ends_at, status)
            SELECT id, ?, ?, ?, ?, ?, 'BOOKED'
            FROM barber
            WHERE barbershop_id = ? AND lower(name) = lower(?) AND lower(surname) = lower(?) AND active
            RETURNING *
            """;
        try {
            return jdbcTemplate.query(sql, rowMapper, clientName, phoneNumber, comment, startsAt, endsAt,
                            barbershopId, barberName, barberSurname)
                    .stream().findFirst()
                    .<BookingResult>map(Booked::new)
                    .orElseGet(BarberNotFound::new);
        } catch (DataAccessException e) {
            if (isSlotConflict(e)) return new SlotTaken();
            throw e;
        }
    }

    Optional<BookingResult> reschedule(long barbershopId, long appointmentId, String phoneNumber,
                                       OffsetDateTime startsAt, OffsetDateTime endsAt) {
        String sql = """
            UPDATE appointment a SET starts_at = ?, ends_at = ?
            FROM barber b
            WHERE a.id = ? AND a.client_phone_number = ? AND a.status = 'BOOKED' AND a.starts_at > now()
              AND b.id = a.barber_id AND b.barbershop_id = ?
            RETURNING a.*
            """;
        try {
            return jdbcTemplate.query(sql, rowMapper, startsAt, endsAt, appointmentId, phoneNumber, barbershopId)
                    .stream().findFirst()
                    .map(Booked::new);
        } catch (DataAccessException e) {
            if (isSlotConflict(e)) return Optional.of(new SlotTaken());
            throw e;
        }
    }

    Optional<Appointment> cancel(long barbershopId, long appointmentId, String phoneNumber) {
        String sql = """
            UPDATE appointment a SET status = 'CANCELLED'
            FROM barber b
            WHERE a.id = ? AND a.client_phone_number = ? AND a.status = 'BOOKED' AND a.starts_at > now()
              AND b.id = a.barber_id AND b.barbershop_id = ?
            RETURNING a.*
            """;
        return jdbcTemplate.query(sql, rowMapper, appointmentId, phoneNumber, barbershopId).stream().findFirst();
    }

    List<ClientAppointment> clientAppointments(long barbershopId, String phoneNumber) {
        String sql = """
            SELECT a.id, b.name || ' ' || b.surname AS barber, a.starts_at
            FROM appointment a JOIN barber b ON b.id = a.barber_id
            WHERE b.barbershop_id = ? AND a.client_phone_number = ? AND a.status = 'BOOKED' AND a.ends_at > now()
            ORDER BY a.starts_at
            """;
        return jdbcTemplate.query(sql,
                (rs, n) -> new ClientAppointment(rs.getLong("id"), rs.getString("barber"),
                        rs.getObject("starts_at", OffsetDateTime.class)),
                barbershopId, phoneNumber);
    }

    List<BusyTime> busyTimes(long barbershopId, String barberName, String barberSurname,
                             OffsetDateTime from, OffsetDateTime to) {
        String sql = """
            SELECT a.starts_at, a.ends_at
            FROM appointment a JOIN barber b ON b.id = a.barber_id
            WHERE b.barbershop_id = ? AND lower(b.name) = lower(?) AND lower(b.surname) = lower(?)
              AND a.status <> 'CANCELLED'
              AND tstzrange(a.starts_at, a.ends_at) && tstzrange(?, ?)
            ORDER BY a.starts_at
            """;
        return jdbcTemplate.query(sql,
                (rs, n) -> new BusyTime(rs.getObject("starts_at", OffsetDateTime.class),
                        rs.getObject("ends_at", OffsetDateTime.class)),
                barbershopId, barberName, barberSurname, from, to);
    }

    private static boolean isSlotConflict(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof PSQLException p && EXCLUSION_VIOLATION.equals(p.getSQLState())) {
                var details = p.getServerErrorMessage();
                return details != null && OVERLAP_CONSTRAINT.equals(details.getConstraint());
            }
        }
        return false;
    }

    private final RowMapper<Appointment> rowMapper =
            (rs, rowNum) -> Appointment.builder()
                    .id(rs.getLong("id"))
                    .clientName(rs.getString("client_name"))
                    .clientPhoneNumber(rs.getString("client_phone_number"))
                    .comment(rs.getString("comment"))
                    .startsAt(rs.getObject("starts_at", OffsetDateTime.class))
                    .status(BookStatus.valueOf(rs.getString("status")))
                    .endsAt(rs.getObject("ends_at", OffsetDateTime.class))
                    .barberId(rs.getLong("barber_id"))
                    .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                    .serviceId(rs.getObject("service_id", Long.class))
                    .build();
}
