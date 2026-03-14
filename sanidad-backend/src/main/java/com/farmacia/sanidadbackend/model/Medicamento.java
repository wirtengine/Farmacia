package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(name = "medicamentos")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registro_sanitario", nullable = false, unique = true)
    private String registroSanitario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String presentacion;

    @Column(nullable = false)
    private String via;

    private String fabricante;

    @Column(name = "tipo_venta")
    private String tipoVenta;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    private Boolean receta;

    @Column(nullable = false)
    private Boolean activo = true;
}