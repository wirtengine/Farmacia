package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroLote;

    private LocalDate fechaFabricacion;

    private LocalDate fechaVencimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(length = 100)
    private String factura;

    @OneToMany(
            mappedBy = "lote",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LoteDetalle> detalles = new ArrayList<>();

    @Column(nullable = false)
    private boolean activo = true;
}