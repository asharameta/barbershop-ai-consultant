package com.asharameta.barbershop.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record Question(@NotBlank @Pattern(regexp = "^[0-9a-f-]{36}$") String conversationId,
                       @NotBlank String question,
                       @NotBlank @Pattern(regexp = "^[\\p{L}0-9 _-]{1,100}$") String barbershopCity) {
}
