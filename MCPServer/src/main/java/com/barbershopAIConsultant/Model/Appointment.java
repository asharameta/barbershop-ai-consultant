package com.barbershopAIConsultant.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import  lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Appointment {
    private int id;
    private String barberName;
    private String clientName;
    private String phoneNumber;
    private String comment;
    private LocalDateTime dateTime;
    private BookStatus status;
}
