package com.asharameta.barbershop.knowledgebase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.joining;


final class StaffParser {
    record StaffMember(String name, String surname, String role, boolean bookable) {}

    private static final Locale PL = Locale.forLanguageTag("pl-PL");
    private static final String DASH = " — ";

    private StaffParser() {}

    static List<StaffMember> parse(String text) {
        List<StaffMember> staff = new ArrayList<>();

        for (String line : text.lines().toList()) {
            int dash = line.indexOf(DASH);
            if (dash < 0) continue;

            String fullName = line.substring(0, dash).strip();
            String role = line.substring(dash + DASH.length()).strip();

            if (!isAllCaps(fullName) || isAllCaps(role) || !fullName.contains(" ")) continue;

            String[] words = fullName.split(" ");
            staff.add(new StaffMember(
                    capitalize(words[0]),
                    Arrays.stream(words).skip(1).map(StaffParser::capitalize).collect(joining(" ")),
                    role,
                    isBarber(role)));
        }
        return staff;
    }

    private static boolean isBarber(String role) {
        String lower = role.toLowerCase(PL);
        return lower.contains("barber") || lower.contains("fryzjer");
    }

    private static boolean isAllCaps(String text) {
        return text.equals(text.toUpperCase(PL));
    }

    private static String capitalize(String word) {
        return Arrays.stream(word.split("-"))
                .map(part -> part.isEmpty() ? part : part.charAt(0) + part.substring(1).toLowerCase(PL))
                .collect(joining("-"));
    }
}
