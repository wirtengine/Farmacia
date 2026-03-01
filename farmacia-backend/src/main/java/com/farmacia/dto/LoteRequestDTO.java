package com.farmacia.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LoteRequestDTO {

    @NotNull(message = "El ID del medicamento es obligatorio")
    private Long medicamentoId;

    @NotBlank(message = "El número de lote es obligatorio")
    @Size(max = 50)
    private String numeroLote;

    @NotNull(message = "La fecha de fabricación es obligatoria")
    private LocalDate fechaFabricacion;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    @NotNull(message = "La cantidad inicial es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidadInicial;

    @NotBlank(message = "El fabricante es obligatorio")
    private String fabricante;

    @NotBlank(message = "El proveedor es obligatorio")
    private String proveedor;

    // Nota: cantidadActual se inicializa igual a cantidadInicial al crear
}