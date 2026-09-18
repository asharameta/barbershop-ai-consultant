package com.asharameta.barbershop.idempotency;

public enum IdempotencyState {
    CLAIMED,
    DUPLICATE_IN_PROGRESS,
    KEY_REUSED,
    COMPLETED,
    LOCK_EXPIRED
}
