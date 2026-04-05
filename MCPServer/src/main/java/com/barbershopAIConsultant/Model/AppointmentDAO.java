package com.barbershopAIConsultant.Model;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@AllArgsConstructor
public class AppointmentDAO {
    private final JdbcTemplate jdbcTemplate;

    public Appointment bookAppointment(Appointment appointment){
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
                BookStatus.BOOKED.ordinal()
        );
    }

    public boolean cancelAppointment(int id){
        String sql = "UPDATE appointments SET status = ? WHERE id = ? AND status = ?";
        return jdbcTemplate.update(sql, BookStatus.CANCELLED.ordinal(), id, BookStatus.BOOKED.ordinal()) == 1;
    }

    public List<Appointment> getClientAppointments(String phoneNumber){
        String sql = "SELECT * FROM appointments WHERE phone_number = ?";

        return jdbcTemplate.query(sql, rowMapper, phoneNumber);
    }

    public List<Appointment> getBarberSchedule(String barberName, String date){
        String sql = "SELECT * FROM appointments WHERE barber_name = ? AND DATE(date_time) = ? AND status = ? ORDER BY date_time";
        LocalDate localDate = LocalDate.parse(date);
        return jdbcTemplate.query(sql, rowMapper, barberName, localDate, BookStatus.BOOKED.ordinal());
    }

    private final RowMapper<Appointment> rowMapper =
            (rs, rowNum) -> Appointment.builder()
                    .id(rs.getInt("id"))
                    .barberName(rs.getString("barber_name"))
                    .clientName(rs.getString("client_name"))
                    .phoneNumber(rs.getString("phone_number"))
                    .comment(rs.getString("comment"))
                    .dateTime(rs.getTimestamp("date_time").toLocalDateTime())
                    .status(BookStatus.values()[rs.getInt("status")])
                    .build();
}
