package com.asharameta.barbershop.model;

public record IdempotencyDTO(
        IdempotencyState state,
        Integer responseStatus,
        String responseBody
) {
}
