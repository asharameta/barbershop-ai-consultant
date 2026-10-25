package com.asharameta.barbershop.appointment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum BookStatus {
    BOOKED("BOOKED"),
    CANCELLED("CANCELLED"),
    UNAVAILABLE("UNAVAILABLE");
    private final String status;

    private static BookStatus getStatus(String bookStatus) {
        return Arrays.stream(BookStatus.values()).filter(status -> status.status.equals(bookStatus)).findFirst().orElseThrow();
    }

}
