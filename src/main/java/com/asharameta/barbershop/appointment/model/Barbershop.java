package com.asharameta.barbershop.appointment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Barbershop {
    private Long id;
    private Long cityId;
    private String name;
    private String slug;
}
