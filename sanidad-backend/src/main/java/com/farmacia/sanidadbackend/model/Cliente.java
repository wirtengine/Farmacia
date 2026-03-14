package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String cedula;  // Puede ser cédula, RUC, etc.

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String email;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;  // Por defecto 0

    @Column(nullable = false)
    private Boolean activo = true;  // Borrado lógico
}