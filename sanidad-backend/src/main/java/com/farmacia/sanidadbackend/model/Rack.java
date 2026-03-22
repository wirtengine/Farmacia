package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "racks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer ancho;

    @Column(nullable = false)
    private Integer alto;

    @Column(nullable = false)
    private Integer profundidad;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}