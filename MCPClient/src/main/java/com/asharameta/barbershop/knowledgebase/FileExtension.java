package com.asharameta.barbershop.knowledgebase;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum FileExtension {
    TXT(".txt");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    boolean matches(String fileName) {
        return fileName.toLowerCase().endsWith(extension);
    }

    static Optional<FileExtension> fromFileName(String fileName) {
        return Arrays.stream(values())
                .filter(ext -> ext.matches(fileName))
                .findFirst();
    }
}
