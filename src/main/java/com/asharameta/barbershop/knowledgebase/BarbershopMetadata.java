package com.asharameta.barbershop.knowledgebase;

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