package com.barbershopAIConsultant.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BarbershopFileParser {
    private static final String SECTION_DELIMITER = "__";

    /**
     * Metadata extracted from barbershop file name
     */
    public record BarbershopMetadata(String barbershopName, String city, String category, String fileName) {

        public String getDisplayName() {
            return formatName(barbershopName);
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
     * Pattern: {barbershop_name}__{city}__{category}.txt
     *
     * @param fileName the file name (e.g., "black_gold__houston__about.txt")
     * @return BarbershopMetadata or null if pattern doesn't match
     */
    public static BarbershopMetadata parseFileName(String fileName) {
        // Remove .txt extension if present
        String nameWithoutExt = fileName;
        if (fileName.endsWith(".txt")) {
            nameWithoutExt = fileName.substring(0, fileName.length() - 4);
        }

        // Split by double underscore
        String[] sections = nameWithoutExt.split(SECTION_DELIMITER);

        // Should have exactly 3 sections: barbershop, city, category
        if (sections.length != 3) {
            return null;
        }

        String barbershopName = sections[0];
        String city = sections[1];
        String category = sections[2];

        // Validate that none of the sections are empty
        if (barbershopName.isEmpty() || city.isEmpty() || category.isEmpty()) {
            return null;
        }

        return new BarbershopMetadata(barbershopName, city, category, fileName);
    }

    /**
     * Extracts metadata from a file path
     *
     * @param filePath the full file path
     * @return BarbershopMetadata or null if pattern doesn't match
     */
    public static BarbershopMetadata parseFilePath(String filePath) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        return parseFileName(fileName);
    }

    /**
     * Extracts just the barbershop name from file name
     */
    public static String extractBarbershopName(String fileName) {
        BarbershopMetadata metadata = parseFileName(fileName);
        return metadata != null ? metadata.barbershopName() : null;
    }

    /**
     * Extracts just the city from file name
     */
    public static String extractCity(String fileName) {
        BarbershopMetadata metadata = parseFileName(fileName);
        return metadata != null ? metadata.city() : null;
    }

    /**
     * Extracts just the category from file name
     */
    public static String extractCategory(String fileName) {
        BarbershopMetadata metadata = parseFileName(fileName);
        return metadata != null ? metadata.category() : null;
    }

    /**
     * Generates a valid file name from components
     *
     * @param barbershopName the barbershop name (can contain single underscores)
     * @param city the city name (can contain single underscores)
     * @param category the category (can contain single underscores)
     * @return formatted file name
     */
    public static String generateFileName(String barbershopName, String city, String category) {
        return barbershopName + SECTION_DELIMITER + city + SECTION_DELIMITER + category + ".txt";
    }
}