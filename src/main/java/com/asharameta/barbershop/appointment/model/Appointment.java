package com.asharameta.barbershop.appointment.model;

import lombok.*;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Appointment {
    private Long id;
    private String clientName;
    private String clientPhoneNumber;
    private String comment;
    private OffsetDateTime startsAt;
    private BookStatus status;
    private OffsetDateTime endsAt;
    private Long barberId;
    private OffsetDateTime createdAt;
    private Long serviceId;
}
