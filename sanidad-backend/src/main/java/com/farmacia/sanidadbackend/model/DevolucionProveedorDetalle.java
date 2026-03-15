package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "devolucion_proveedor_detalle")
@Data
@NoArgsConstructor
public class DevolucionProveedorDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucion_proveedor_id", nullable = false)
    private DevolucionProveedor devolucionProveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_detalle_id", nullable = false)
    private LoteDetalle loteDetalle;

    @Column(nullable = false)
    private Integer cantidadDevuelta;
}