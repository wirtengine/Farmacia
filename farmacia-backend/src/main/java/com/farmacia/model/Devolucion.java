package com.farmacia.model;

import com.farmacia.enums.EstadoDevolucion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "devoluciones")
@Data
@NoArgsConstructor
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_id")
    private VentaDetalle detalle; // puede ser null si es devolución total

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario admin; // quien aprueba/rechaza

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoDevolucion estado;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaAprobacion;

    @Column(length = 500)
    private String observacionAdmin;
}