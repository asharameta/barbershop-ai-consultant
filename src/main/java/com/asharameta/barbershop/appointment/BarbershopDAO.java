package com.asharameta.barbershop.appointment;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class BarbershopDAO {
    public record Shop(long id, String slug, String citySlug) {}

    private final JdbcTemplate jdbcTemplate;

    public Optional<Shop> find(String city, String shop) {
        String sql = """
            SELECT b.id, b.slug, c.slug AS city_slug
            FROM barbershop b JOIN city c ON c.id = b.city_id
            WHERE c.slug = slugify(?) AND b.slug = slugify(?)
            """;
        return jdbcTemplate.query(sql,
                (rs, n) -> new Shop(rs.getLong("id"), rs.getString("slug"), rs.getString("city_slug")),
                city, shop).stream().findFirst();
    }
}
