package com.asharameta.barbershop.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record Question(UUID conversationId,
                @NotBlank @Size(max=300) String question,
                @NotBlank @Size(max=50) @Pattern(regexp = "^[\\p{L}0-9 _-]{1,100}$") String barbershopCity) {}
