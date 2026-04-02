package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.repository.VentaDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrediccionService {

    private final VentaDetalleRepository ventaDetalleRepository;

    /**
     * Predice las ventas de un medicamento para los próximos N días.
     * Utiliza regresión lineal sobre las ventas diarias de los últimos 60 días.
     * @param medicamentoId ID del medicamento
     * @param diasHorizonte número de días a predecir
     * @return mapa con predicción por día y total
     */
    public Map<String, Object> predecirVentas(Long medicamentoId, int diasHorizonte) {
        // Obtener ventas diarias de los últimos 60 días
        LocalDateTime inicio = LocalDateTime.now().minusDays(60);
        LocalDateTime fin = LocalDateTime.now();
        List<Object[]> ventasDiarias = ventaDetalleRepository.sumCantidadDiariaPorMedicamento(medicamentoId, inicio, fin);

        if (ventasDiarias.size() < 7) {
            return Map.of("error", "Datos insuficientes para predicción (mínimo 7 días de ventas).");
        }

        // Preparar datos para regresión: x = día (1,2,3...), y = cantidad vendida
        SimpleRegression reg = new SimpleRegression();
        Map<LocalDate, Integer> mapa = new HashMap<>();
        for (Object[] row : ventasDiarias) {
            LocalDate fecha = (LocalDate) row[0];
            int cantidad = ((Number) row[1]).intValue();
            mapa.put(fecha, cantidad);
        }

        // Ordenar fechas
        List<LocalDate> fechas = new ArrayList<>(mapa.keySet());
        Collections.sort(fechas);
        LocalDate fechaBase = fechas.get(0);
        for (int i = 0; i < fechas.size(); i++) {
            double x = i + 1; // día desde el inicio
            double y = mapa.get(fechas.get(i));
            reg.addData(x, y);
        }

        // Predecir
        List<Map<String, Object>> predicciones = new ArrayList<>();
        double totalPredicho = 0;
        for (int i = 1; i <= diasHorizonte; i++) {
            double x = fechas.size() + i;
            double yPred = reg.predict(x);
            yPred = Math.max(0, yPred); // evitar negativos
            totalPredicho += yPred;
            predicciones.add(Map.of(
                    "dia", i,
                    "prediccion", Math.round(yPred)
            ));
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("predicciones", predicciones);
        resultado.put("totalPredicho", Math.round(totalPredicho));
        resultado.put("coeficienteDeterminacion", reg.getRSquare()); // R² como medida de confianza
        return resultado;
    }
}