package com.farmacia.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "medicamentos")
@Data
@NoArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String principioActivo;

    @Column(nullable = false, length = 50)
    private String presentacion; // ej: "Tabletas 500 mg", "Jarabe 120 ml"

    @Column(nullable = false, length = 20)
    private String viaAdministracion; // ej: "Oral", "Tópica", "Intramuscular"

    @Column(nullable = false, length = 50)
    private String fabricante;

    @Column(nullable = false, unique = true, length = 20)
    private String registroSanitario; // Número de registro en el Ministerio de Salud (Nicaragua)

    @Column(nullable = false)
    private Boolean requiereReceta; // true si es con receta médica

    @Column(nullable = false, length = 20)
    private String tipoVenta; // ej: "Libre", "Controlado", "Psicotrópico"

    @Column(nullable = false)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Integer stockMaximo;

    @Column(nullable = false)
    private Boolean activo; // para borrado lógico
}