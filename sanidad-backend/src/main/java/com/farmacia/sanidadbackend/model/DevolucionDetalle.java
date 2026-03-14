package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "devolucion_detalle")
@Data
@NoArgsConstructor
public class DevolucionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_detalle_id", nullable = false)
    private VentaDetalle ventaDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_detalle_id", nullable = false)
    private LoteDetalle loteDetalle;

    @Column(nullable = false)
    private Integer cantidadDevuelta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}