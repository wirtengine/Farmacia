package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Imagen subida por el farmacéutico
    @Column(nullable = false)
    private String imagen;

    // Fecha de subida
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    // Farmacéutico que la subió (puede ser también quien la valida)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmaceutico_id")
    private Usuario farmaceutico;

    // Estado de validación manual
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReceta estado = EstadoReceta.PENDIENTE;

    // Relación opcional con la venta (se asigna cuando se usa en una venta)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;
}