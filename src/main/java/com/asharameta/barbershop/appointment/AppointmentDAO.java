package com.asharameta.barbershop.appointment;

import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@AllArgsConstructor
class AppointmentDAO {
    private final JdbcTemplate jdbcTemplate;

    Appointment bookAppointment(Appointment appointment){
        try{
           return insertAppointment(appointment);
        }catch (DuplicateKeyException e){
            return Appointment.builder().status(BookStatus.UNAVAILABLE).build();
        }
    }

    private Appointment insertAppointment(Appointment appointment){
        String sql = """
            INSERT INTO appointments (barber_name, client_name, phone_number, comment, date_time, status)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING *
            """;

        return jdbcTemplate.queryForObject(sql,
                rowMapper,
                appointment.getBarberName(),
                appointment.getClientName(),
                appointment.getPhoneNumber(),
                appointment.getComment(),
                appointment.getDateTime(),
                BookStatus.BOOKED.name()
        );
    }

    @Transactional
    Appointment rescheduleAppointment(String clientName, String phoneNumber, Appointment newAppointment){
        var cancelledAppointment = cancelAppointment(clientName, phoneNumber);
        if(cancelledAppointment == null){
            return null;
        }
        newAppointment.setBarberName(cancelledAppointment.getBarberName());
        newAppointment.setComment(cancelledAppointment.getComment());
        return bookAppointment(newAppointment);
    }

    Appointment cancelAppointment(String clientName, String phoneNumber){
        String sql = "UPDATE appointments SET status = ? WHERE client_name = ? AND phone_number = ? AND status = ? RETURNING *";
        try{
            return jdbcTemplate.queryForObject(sql,
                    rowMapper,
                    BookStatus.CANCELLED.name(),
                    clientName,
                    phoneNumber,
                    BookStatus.BOOKED.name());
        }catch(EmptyResultDataAccessException ex){
            return null;
        }

    }

    List<Appointment> getClientAppointments(String phoneNumber){
        String sql = "SELECT * FROM appointments WHERE phone_number = ?";

        return jdbcTemplate.query(sql, rowMapper, phoneNumber);
    }

    List<Appointment> getBarberSchedule(String barberName, String date){
        String sql = "SELECT barber_name, date_time FROM appointments WHERE barber_name = ? AND DATE(date_time) = ? AND status = ? ORDER BY date_time";
        LocalDate localDate = LocalDate.parse(date);
        return jdbcTemplate.query(sql, rowMapper, barberName, localDate, BookStatus.BOOKED.name());
    }

    private final RowMapper<Appointment> rowMapper =
            (rs, rowNum) -> Appointment.builder()
                    .id(rs.getInt("id"))
                    .barberName(rs.getString("barber_name"))
                    .clientName(rs.getString("client_name"))
                    .phoneNumber(rs.getString("phone_number"))
                    .comment(rs.getString("comment"))
                    .dateTime(rs.getTimestamp("date_time").toLocalDateTime())
                    .status(BookStatus.valueOf(rs.getString("status")))
                    .build();
}
