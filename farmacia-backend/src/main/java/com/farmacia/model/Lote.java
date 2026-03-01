package com.farmacia.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes")
@Data
@NoArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private LocalDate fechaFabricacion;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    private Integer cantidadInicial;

    @Column(nullable = false)
    private Integer cantidadActual;

    @Column(nullable = false)
    private String fabricante;

    @Column(nullable = false)
    private String proveedor;

    @Column(nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(nullable = false)
    private Boolean activo = true;

    // Precio de compra del lote (ahora puede ser nulo temporalmente)
    @Column(precision = 10, scale = 2)
    private BigDecimal precioCompra;

    public boolean isProximoAVencer(int diasLimite) {
        return LocalDate.now().plusDays(diasLimite).isAfter(fechaVencimiento);
    }
}