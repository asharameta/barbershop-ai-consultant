package com.asharameta.barbershop.idempotency;

public record IdempotencyDTO(
        IdempotencyState state,
        Integer responseStatus,
        String responseBody
) {
}
