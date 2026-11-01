package com.asharameta.barbershop.knowledgebase;

import com.asharameta.barbershop.knowledgebase.StaffParser.StaffMember;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaffParserTest {

    private static List<StaffMember> parseFile(String path) throws IOException {
        try (var in = StaffParserTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return StaffParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static List<String> names(List<StaffMember> members) {
        return members.stream().map(m -> m.name() + " " + m.surname()).toList();
    }

    @Test
    void parsesStaryCyrulik() throws IOException {
        var staff = parseFile("/docs/stary-cyrulik/stary-cyrulik__gdansk__staff.txt");

        assertEquals(List.of("Marek Wach", "Tomasz Nowicki", "Ola Bereza"), names(staff));
        assertTrue(staff.stream().allMatch(StaffMember::bookable));
        assertEquals("właściciel, mistrz fryzjerski", staff.getFirst().role());
    }

    @Test
    void parsesBrzytwaAndSkipsTheTitleLine() throws IOException {
        var staff = parseFile("/docs/brzytwa-i-was/brzytwa-i-was__poznan__staff.txt");

        assertEquals(List.of("Kuba Sikora", "Dawid Malinowski", "Nina Górecka", "Patryk Wiśniewski"), names(staff));
        assertTrue(staff.stream().allMatch(StaffMember::bookable));
    }

    @Test
    void creativeDirectorIsNotBookable() throws IOException {
        var staff = parseFile("/docs/cechownia/cechownia__wroclaw__staff.txt");

        assertEquals(List.of("Michał Król", "Aleksander Duda", "Weronika Sadowska", "Bartosz Olejnik"), names(staff));
        assertFalse(staff.getFirst().bookable());
        assertTrue(staff.stream().skip(1).allMatch(StaffMember::bookable));
        assertEquals("lead barber, Nadodrze", staff.get(1).role());
    }

    @Test
    void ignoresProseAndHeadings() {
        var staff = StaffParser.parse("""
                SOME SHOP — ZESPÓŁ

                ANNA KOWALSKA-NOWAK — barberka
                Specjalizuje się w fade'ach — od 2019 roku.

                WYBÓR BARBERA
                """);

        assertEquals(1, staff.size());
        assertEquals("Anna", staff.getFirst().name());
        assertEquals("Kowalska-Nowak", staff.getFirst().surname());
    }
}
