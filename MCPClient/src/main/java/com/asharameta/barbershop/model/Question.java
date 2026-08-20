package com.asharameta.barbershop.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Question(@NotBlank @Pattern(regexp = "^[0-9a-f-]{36}$") String conversationId, @NotBlank String question, @NotBlank String barbershopName, @NotBlank String barbershopCity) {
}
