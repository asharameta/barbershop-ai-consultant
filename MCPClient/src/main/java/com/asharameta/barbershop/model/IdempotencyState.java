package com.asharameta.barbershop.model;

public enum IdempotencyState {
    CLAIMED,
    DUPLICATE_IN_PROGRESS,
    KEY_REUSED,
    COMPLETED,
    LOCK_EXPIRED
}
