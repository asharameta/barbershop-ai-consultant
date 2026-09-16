package com.asharameta.barbershop.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Question(@NotBlank @Pattern(regexp = "^[0-9a-f-]{36}$") String conversationId,
                       @NotBlank String question,
                       @NotBlank @Pattern(regexp = "^[\\p{L}0-9 _-]{1,100}$") String barbershopName,
                       @NotBlank @Pattern(regexp = "^[\\p{L}0-9 _-]{1,100}$") String barbershopCity) {
}
