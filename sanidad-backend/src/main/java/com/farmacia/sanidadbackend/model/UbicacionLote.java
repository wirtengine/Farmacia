package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ubicaciones_lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rack_id", nullable = false)
    private Rack rack;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_detalle_id", nullable = false)
    private LoteDetalle loteDetalle;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false)
    private Integer columna;

    @Column(nullable = false)
    private Integer profundidadIndex;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}