package com.asharameta.barbershop.appointment.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BarbershopService {
    private Long id;
    private String name;
    private int durationMinutes;
    private BigDecimal price;
}
