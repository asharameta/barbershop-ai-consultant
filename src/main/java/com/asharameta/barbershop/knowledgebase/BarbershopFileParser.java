package com.asharameta.barbershop.knowledgebase;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BarbershopFileParser {
    private static final Pattern SECTIONS_PATTERN = Pattern.compile("^([^_]+)__([^_]+)__([^_]+)$");

    /**
     * Metadata extracted from barbershop file name
     */
    public record BarbershopMetadata(String name, String city, String category, String fileName) {

        public String getDisplayName() {
            return formatName(name);
        }

        public String getDisplayCity() {
            return formatName(city);
        }

        public String getDisplayCategory() {
            return formatName(category);
        }

        private String formatName(String underscoreName) {
            String[] parts = underscoreName.split("_");
            StringBuilder result = new StringBuilder();

            for (String part : parts) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }
                result.append(capitalize(part));
            }

            return result.toString();
        }

        private String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        @Override
        public String toString() {
            return String.format("BarbershopMetadata{barbershop='%s', city='%s', category='%s'}",
                    getDisplayName(), getDisplayCity(), getDisplayCategory());
        }
    }

    /**
     * Extracts metadata from a barbershop file name
     * First Possible Pattern: {name}__{city}__{category}.txt
     * Second Possible Pattern: {barbershop-name}__{city-name}__{category}.txt
     *
     * @param fileName the file name (e.g., "black-gold__houston__about.txt", "sharp__new-york__staff.txt")
     * @return BarbershopMetadata or null if pattern doesn't match
     */
    public static BarbershopMetadata parseFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        Optional<FileExtension> extension = FileExtension.fromFileName(fileName);
        if (extension.isEmpty()) {
            return null; // rejects missing/wrong extension
        }

        String nameWithoutExt = fileName.substring(0, fileName.length() - extension.get().getExtension().length());

        // reject double extensions like "category.txt.txt"
        if (FileExtension.fromFileName(nameWithoutExt).isPresent()) {
            return null;
        }

        Matcher matcher = SECTIONS_PATTERN.matcher(nameWithoutExt);
        if (!matcher.matches()) {
            return null; // rejects wrong delimiter count, extra underscores, single underscores
        }

        return new BarbershopMetadata(matcher.group(1), matcher.group(2), matcher.group(3), fileName);
    }
}