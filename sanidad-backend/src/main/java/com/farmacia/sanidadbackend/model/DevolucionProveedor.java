package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devoluciones_proveedor")
@Data
@NoArgsConstructor
public class DevolucionProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String numeroDevolucion; // Se genera al aprobar

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucionProveedor estado;

    @Column(length = 500)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaAprobacion;

    @OneToMany(mappedBy = "devolucionProveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DevolucionProveedorDetalle> detalles = new ArrayList<>();
}