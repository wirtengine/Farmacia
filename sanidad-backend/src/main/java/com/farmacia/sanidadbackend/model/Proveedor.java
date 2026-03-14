package com.farmacia.sanidadbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proveedores")
@Data
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String ruc;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String email;

    @Column(nullable = false)
    private Boolean activo = true;  // Borrado lógico
}