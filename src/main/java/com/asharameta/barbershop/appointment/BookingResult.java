package com.asharameta.barbershop.appointment;

sealed interface BookingResult {
    record Booked(Appointment appointment) implements BookingResult {}
    record SlotTaken() implements BookingResult {}
    record BarberNotFound() implements BookingResult {}
}
