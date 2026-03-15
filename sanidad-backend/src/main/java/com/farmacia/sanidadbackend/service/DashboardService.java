package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.Rol;
import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.repository.LoteDetalleRepository;
import com.farmacia.sanidadbackend.repository.VentaDetalleRepository;
import com.farmacia.sanidadbackend.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final LoteDetalleRepository loteDetalleRepository;

    public DashboardResponseDTO obtenerResumen(Usuario usuario) {

        boolean esAdmin = usuario.getRol() == Rol.ADMIN;
        Long usuarioId = usuario.getId();

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> ventasDia;
        if (esAdmin) {
            ventasDia = ventaRepository.findVentasDelDia(inicioDia, finDia);
        } else {
            ventasDia = ventaRepository.findVentasDelDiaByUsuario(inicioDia, finDia, usuarioId);
        }

        VentasDelDiaDTO ventasDelDia;
        if (ventasDia.isEmpty()) {
            ventasDelDia = new VentasDelDiaDTO(0, BigDecimal.ZERO);
        } else {
            Object[] row = ventasDia.get(0);
            long cantidad = ((Number) row[0]).longValue();
            BigDecimal total = (BigDecimal) row[1];
            ventasDelDia = new VentasDelDiaDTO(cantidad, total);
        }

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMesActual = hoy.withDayOfMonth(1);
        LocalDate finMesActual = hoy.withDayOfMonth(hoy.lengthOfMonth());
        LocalDate inicioMesAnterior = inicioMesActual.minusMonths(1);
        LocalDate finMesAnterior = inicioMesActual.minusDays(1);

        BigDecimal ventasMesActual;
        BigDecimal ventasMesAnterior;

        if (esAdmin) {
            ventasMesActual = ventaRepository.sumVentasByPeriodo(
                    inicioMesActual.atStartOfDay(),
                    finMesActual.atTime(LocalTime.MAX)
            );

            ventasMesAnterior = ventaRepository.sumVentasByPeriodo(
                    inicioMesAnterior.atStartOfDay(),
                    finMesAnterior.atTime(LocalTime.MAX)
            );
        } else {
            ventasMesActual = ventaRepository.sumVentasByPeriodoAndUsuario(
                    inicioMesActual.atStartOfDay(),
                    finMesActual.atTime(LocalTime.MAX),
                    usuarioId
            );

            ventasMesAnterior = ventaRepository.sumVentasByPeriodoAndUsuario(
                    inicioMesAnterior.atStartOfDay(),
                    finMesAnterior.atTime(LocalTime.MAX),
                    usuarioId
            );
        }

        List<ProductoRankingDTO> productosMasRentables = Collections.emptyList();

        if (esAdmin) {
            List<Object[]> topProductos = ventaDetalleRepository.findTopProductosByIngresos();

            productosMasRentables = topProductos.stream()
                    .limit(5)
                    .map(row -> new ProductoRankingDTO((String) row[0], (BigDecimal) row[1]))
                    .collect(Collectors.toList());
        }

        List<ProductoStockDTO> productosBajoStock = Collections.emptyList();

        if (esAdmin) {
            List<Object[]> bajoStock = loteDetalleRepository.findProductosBajoStock();

            productosBajoStock = bajoStock.stream()
                    .limit(5)
                    .map(row -> new ProductoStockDTO((String) row[0], ((Number) row[1]).intValue()))
                    .collect(Collectors.toList());
        }

        List<VendedorRankingDTO> rankingVendedores = Collections.emptyList();

        if (esAdmin) {
            List<Object[]> ranking = ventaRepository.findRankingVendedores();

            rankingVendedores = ranking.stream()
                    .map(row -> new VendedorRankingDTO(
                            (String) row[0],
                            ((Number) row[1]).longValue(),
                            (BigDecimal) row[2]
                    ))
                    .collect(Collectors.toList());
        } else {
            BigDecimal totalVentasVendedor = ventaRepository.sumVentasByUsuario(usuarioId);
            long cantidadVentasVendedor = ventaRepository.countVentasByUsuario(usuarioId);

            rankingVendedores = List.of(
                    new VendedorRankingDTO(
                            usuario.getUsername(),
                            cantidadVentasVendedor,
                            totalVentasVendedor
                    )
            );
        }

        return new DashboardResponseDTO(
                ventasDelDia,
                productosMasRentables,
                productosBajoStock,
                rankingVendedores,
                ventasMesActual != null ? ventasMesActual : BigDecimal.ZERO,
                ventasMesAnterior != null ? ventasMesAnterior : BigDecimal.ZERO
        );
    }
}