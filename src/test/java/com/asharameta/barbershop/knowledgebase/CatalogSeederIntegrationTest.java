package com.asharameta.barbershop.knowledgebase;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CatalogSeederIntegrationTest {
    private static PostgreSQLContainer postgres;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void startDatabase() {
        postgres = new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
        postgres.start();
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void stopDatabase() {
        postgres.stop();
    }

    private static long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test
    void seedsTheKnowledgeBaseAndANewStartDoesNotDuplicateAnything() throws Exception {
        var seeder = new CatalogSeeder(jdbc, new PathMatchingResourcePatternResolver(), "classpath:/docs/**/*");

        seeder.run();
        seeder.run(); // simulates the next application start

        assertEquals(3, count("SELECT count(*) FROM city"));
        assertEquals(3, count("SELECT count(*) FROM barbershop"));
        assertEquals(11, count("SELECT count(*) FROM barber"));
        assertEquals(10, count("SELECT count(*) FROM barber WHERE active"));
        assertFalse(jdbc.queryForObject("SELECT active FROM barber WHERE surname = 'Król'", Boolean.class));
        assertEquals(3, count("""
                SELECT count(*) FROM barber b
                JOIN barbershop s ON s.id = b.barbershop_id JOIN city c ON c.id = s.city_id
                WHERE c.slug = 'gdansk' AND s.slug = 'stary-cyrulik' AND b.active
                """));
    }
}
