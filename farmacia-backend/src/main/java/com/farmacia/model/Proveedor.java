package com.farmacia.model;

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

    @Column(nullable = false, length = 100)
    private String nombre; // Razón social o nombre comercial

    @Column(nullable = false, length = 20, unique = true)
    private String ruc; // Registro Único de Contribuyente (Nicaragua)

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String direccion;

    @Column(length = 100)
    private String contacto; // Nombre de la persona de contacto

    @Column(length = 20)
    private String telefonoContacto;

    @Column(nullable = false)
    private Boolean activo = true; // Borrado lógico

    // Nota: Podemos agregar más campos si es necesario, como página web, notas, etc.
}