package com.asharameta.barbershop.appointment;

import com.asharameta.barbershop.appointment.BookingResult.BarberNotFound;
import com.asharameta.barbershop.appointment.BookingResult.Booked;
import com.asharameta.barbershop.appointment.BookingResult.SlotTaken;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDAOIntegrationTest {
    private static final String PHONE = "+48500100200";

    private static PostgreSQLContainer postgres;
    private static JdbcTemplate jdbc;
    private static AppointmentDAO dao;
    private static long shop;
    private static long otherShop;

    @BeforeAll
    static void startDatabase() {
        postgres = new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
        postgres.start();
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        dao = new AppointmentDAO(jdbc);

        jdbc.update("INSERT INTO city (name) VALUES ('gdansk'), ('wroclaw')");
        jdbc.update("INSERT INTO barbershop (city_id, name) SELECT id, 'stary-cyrulik' FROM city WHERE slug = 'gdansk'");
        jdbc.update("INSERT INTO barbershop (city_id, name) SELECT id, 'cechownia' FROM city WHERE slug = 'wroclaw'");
        shop = jdbc.queryForObject("SELECT id FROM barbershop WHERE slug = 'stary-cyrulik'", Long.class);
        otherShop = jdbc.queryForObject("SELECT id FROM barbershop WHERE slug = 'cechownia'", Long.class);
        for (String[] b : new String[][]{{"Tomasz", "Nowicki"}, {"Ola", "Bereza"}}) {
            jdbc.update("INSERT INTO barber (barbershop_id, name, surname, role) VALUES (?, ?, ?, 'barber')", shop, b[0], b[1]);
        }
        jdbc.update("INSERT INTO barber (barbershop_id, name, surname, role, active) VALUES (?, 'Jan', 'Testowy', 'barber', false)", shop);
        jdbc.update("INSERT INTO barber (barbershop_id, name, surname, role) VALUES (?, 'Tomasz', 'Nowicki', 'barber')", otherShop);
    }

    @AfterAll
    static void stopDatabase() {
        postgres.stop();
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM appointment");
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2040, 1, 10, hour, minute, 0, 0, ZoneOffset.ofHours(1));
    }

    private BookingResult book(long shopId, String name, String surname, OffsetDateTime start) {
        return dao.book(shopId, name, surname, "Client", PHONE, null, start, start.plusMinutes(45));
    }

    private long bookedId(BookingResult result) {
        return ((Booked) result).appointment().getId();
    }

    @Test
    void bookingAFreeSlotStoresIt() {
        var result = assertInstanceOf(Booked.class, book(shop, "Tomasz", "Nowicki", at(14, 0)));

        assertEquals(BookStatus.BOOKED, result.appointment().getStatus());
        assertEquals(at(14, 45).toInstant(), result.appointment().getEndsAt().toInstant());
        assertNull(result.appointment().getServiceId());
    }

    @Test
    void overlappingSlotOfTheSameBarberIsRejected() {
        book(shop, "Tomasz", "Nowicki", at(14, 0));

        assertInstanceOf(SlotTaken.class, book(shop, "Tomasz", "Nowicki", at(14, 30)));
        assertInstanceOf(SlotTaken.class, book(shop, "Tomasz", "Nowicki", at(13, 30)));
        assertInstanceOf(SlotTaken.class, book(shop, "Tomasz", "Nowicki", at(14, 0)));
    }

    @Test
    void backToBackSlotsAreAllowed() {
        assertInstanceOf(Booked.class, book(shop, "Tomasz", "Nowicki", at(14, 0)));
        assertInstanceOf(Booked.class, book(shop, "Tomasz", "Nowicki", at(14, 45)));
        assertInstanceOf(Booked.class, book(shop, "Tomasz", "Nowicki", at(13, 15)));
    }

    @Test
    void otherBarbersAndOtherShopsAreIndependent() {
        book(shop, "Tomasz", "Nowicki", at(14, 0));

        assertInstanceOf(Booked.class, book(shop, "Ola", "Bereza", at(14, 0)));
        assertInstanceOf(Booked.class, book(otherShop, "Tomasz", "Nowicki", at(14, 0)));
    }

    @Test
    void barberLookupIsScopedToTheShopAndIgnoresCase() {
        assertInstanceOf(BarberNotFound.class, book(otherShop, "Ola", "Bereza", at(14, 0)));
        assertInstanceOf(Booked.class, book(shop, "tomasz", "NOWICKI", at(15, 0)));
        assertInstanceOf(BarberNotFound.class, book(shop, "Nobody", "Here", at(15, 0)));
    }

    @Test
    void inactiveBarberCannotBeBooked() {
        assertInstanceOf(BarberNotFound.class, book(shop, "Jan", "Testowy", at(14, 0)));
    }

    @Test
    void cancelledSlotCanBeBookedAgain() {
        long id = bookedId(book(shop, "Tomasz", "Nowicki", at(14, 0)));

        assertTrue(dao.cancel(shop, id, PHONE).isPresent());

        assertInstanceOf(Booked.class, book(shop, "Tomasz", "Nowicki", at(14, 0)));
    }

    @Test
    void cancelNeedsTheRightPhoneShopAndABookedAppointment() {
        long id = bookedId(book(shop, "Tomasz", "Nowicki", at(14, 0)));

        assertTrue(dao.cancel(shop, id, "+48999999999").isEmpty());
        assertTrue(dao.cancel(otherShop, id, PHONE).isEmpty());
        assertTrue(dao.cancel(shop, id, PHONE).isPresent());
        assertTrue(dao.cancel(shop, id, PHONE).isEmpty()); // already cancelled
    }

    @Test
    void rescheduleMovesAppointmentEvenOntoItsOwnOldRange() {
        long id = bookedId(book(shop, "Tomasz", "Nowicki", at(14, 0)));

        var moved = dao.reschedule(shop, id, PHONE, at(14, 15), at(15, 0));

        var booked = assertInstanceOf(Booked.class, moved.orElseThrow());
        assertEquals(id, booked.appointment().getId());
        assertEquals(at(14, 15).toInstant(), booked.appointment().getStartsAt().toInstant());
    }

    @Test
    void rescheduleOntoATakenSlotKeepsTheOriginal() {
        long id = bookedId(book(shop, "Tomasz", "Nowicki", at(14, 0)));
        book(shop, "Tomasz", "Nowicki", at(16, 0));

        Optional<BookingResult> moved = dao.reschedule(shop, id, PHONE, at(16, 30), at(17, 15));

        assertInstanceOf(SlotTaken.class, moved.orElseThrow());
        assertEquals(at(14, 0).toInstant(),
                jdbc.queryForObject("SELECT starts_at FROM appointment WHERE id = ?", OffsetDateTime.class, id).toInstant());
    }

    @Test
    void rescheduleOfUnknownOrForeignAppointmentIsEmpty() {
        long id = bookedId(book(shop, "Tomasz", "Nowicki", at(14, 0)));

        assertTrue(dao.reschedule(shop, id, "+48999999999", at(15, 0), at(15, 45)).isEmpty());
        assertTrue(dao.reschedule(otherShop, id, PHONE, at(15, 0), at(15, 45)).isEmpty());
        assertTrue(dao.reschedule(shop, -1, PHONE, at(15, 0), at(15, 45)).isEmpty());
    }

    @Test
    void clientSeesOnlyOwnUpcomingBookingsInThisShop() {
        book(shop, "Tomasz", "Nowicki", at(14, 0));
        long cancelled = bookedId(book(shop, "Ola", "Bereza", at(15, 0)));
        dao.cancel(shop, cancelled, PHONE);

        var list = dao.clientAppointments(shop, PHONE);

        assertEquals(1, list.size());
        assertEquals("Tomasz Nowicki", list.getFirst().barber());
        assertTrue(dao.clientAppointments(otherShop, PHONE).isEmpty());
    }

    @Test
    void busyTimesExposeOnlyTimesOfActiveBookingsInTheWindow() {
        book(shop, "Tomasz", "Nowicki", at(11, 0));
        long cancelled = bookedId(book(shop, "Tomasz", "Nowicki", at(12, 0)));
        dao.cancel(shop, cancelled, PHONE);
        book(shop, "Ola", "Bereza", at(11, 0)); // other barber
        dao.book(shop, "Tomasz", "Nowicki", "Client", PHONE, null, at(9, 0).plusDays(1), at(9, 45).plusDays(1)); // next day

        var busy = dao.busyTimes(shop, "tomasz", "nowicki", at(0, 0), at(0, 0).plusDays(1));

        assertEquals(1, busy.size());
        assertEquals(at(11, 0).toInstant(), busy.getFirst().startsAt().toInstant());
        assertEquals(at(11, 45).toInstant(), busy.getFirst().endsAt().toInstant());
    }

    @Test
    void twentyConcurrentBookingsOfTheSameSlotYieldExactlyOneWinner() throws Exception {
        long booked;
        long taken;
        try (ExecutorService pool = Executors.newFixedThreadPool(20)) {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<BookingResult>> futures = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return book(shop, "Tomasz", "Nowicki", at(14, 0));
                }));
            }
            go.countDown();

            booked = 0;
            taken = 0;
            for (var f : futures) {
                switch (f.get()) {
                    case Booked _ -> booked++;
                    case SlotTaken _ -> taken++;
                    case BarberNotFound _ -> fail("barber exists");
                }
            }
            pool.shutdown();
        }

        assertEquals(1, booked);
        assertEquals(19, taken);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM appointment", Long.class));
    }
}
