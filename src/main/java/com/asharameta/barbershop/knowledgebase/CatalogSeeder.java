package com.asharameta.barbershop.knowledgebase;

import com.asharameta.barbershop.knowledgebase.StaffParser.StaffMember;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Component
@Order(1) // before the vector-store ingest, which needs the embedding API
class CatalogSeeder implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final ResourcePatternResolver resolver;
    private final String resourcePattern;

    CatalogSeeder(JdbcTemplate jdbc, ResourcePatternResolver resolver,
                  @Value("${asharameta.barbershop.knowledge-base.resource-pattern}") String resourcePattern) {
        this.jdbc = jdbc;
        this.resolver = resolver;
        this.resourcePattern = resourcePattern;
    }

    @Override
    public void run(String... args) throws Exception {
        for (Resource file : resolver.getResources(resourcePattern)) {
            // the file name {shop}__{city}__{category}.txt tells which shop the file belongs to
            BarbershopMetadata meta = BarbershopFileParser.parseFileName(file.getFilename());
            if (meta == null) continue; // KnowledgeBaseLoader rejects badly named files

            long shopId = saveShop(meta.name(), meta.city());

            if ("staff".equalsIgnoreCase(meta.category())) {
                List<StaffMember> staff = StaffParser.parse(file.getContentAsString(StandardCharsets.UTF_8));
                if (staff.isEmpty()) {
                    throw new IllegalStateException("No staff found in " + file.getFilename()
                            + " (expected lines like 'NAME SURNAME — role')");
                }
                staff.forEach(member -> saveBarber(shopId, member));
            }
        }
    }

    private long saveShop(String shop, String city) {
        jdbc.update("INSERT INTO city (name) VALUES (?) ON CONFLICT DO NOTHING", city);
        jdbc.update("INSERT INTO barbershop (city_id, name) SELECT id, ? FROM city WHERE slug = slugify(?) ON CONFLICT DO NOTHING",
                shop, city);
        return jdbc.queryForObject("""
                SELECT b.id FROM barbershop b JOIN city c ON c.id = b.city_id
                WHERE c.slug = slugify(?) AND b.slug = slugify(?)
                """, Long.class, city, shop);
    }

    private void saveBarber(long shopId, StaffMember m) {
        int updated = jdbc.update("""
                UPDATE barber SET role = ?, active = ?
                WHERE barbershop_id = ? AND lower(name) = lower(?) AND lower(surname) = lower(?)
                """, m.role(), m.bookable(), shopId, m.name(), m.surname());
        if (updated == 0) {
            jdbc.update("INSERT INTO barber (barbershop_id, name, surname, role, active) VALUES (?, ?, ?, ?, ?)",
                    shopId, m.name(), m.surname(), m.role(), m.bookable());
        }
    }
}
