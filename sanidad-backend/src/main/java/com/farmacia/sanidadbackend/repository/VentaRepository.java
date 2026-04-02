package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByActivoTrue();

    Optional<Venta> findByIdAndActivoTrue(Long id);

    Optional<Venta> findByNumeroFactura(String numeroFactura);

    boolean existsByNumeroFactura(String numeroFactura);

    // 🔥 NUEVO: buscar ventas por rango de fechas (solo activas)
    List<Venta> findByFechaBetweenAndActivoTrue(LocalDateTime inicio, LocalDateTime fin);

    // 🔥 Método para obtener las últimas 5 ventas activas
    List<Venta> findTop5ByActivoTrueOrderByFechaDesc();

    @Query("SELECT COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true")
    List<Object[]> findVentasDelDia(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true AND v.usuario.id = :usuarioId")
    List<Object[]> findVentasDelDiaByUsuario(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true")
    BigDecimal sumVentasByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true AND v.usuario.id = :usuarioId")
    BigDecimal sumVentasByPeriodoAndUsuario(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.usuario.id = :usuarioId AND v.activo = true")
    BigDecimal sumVentasByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.usuario.id = :usuarioId AND v.activo = true")
    long countVentasByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT v.usuario.username, COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.activo = true GROUP BY v.usuario.username ORDER BY SUM(v.total) DESC")
    List<Object[]> findRankingVendedores();

    // ================== MÉTODOS INTELIGENTES ==================

    @Query("SELECT COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true AND (:idUsuario IS NULL OR v.usuario.id = :idUsuario)")
    List<Object[]> findVentasByPeriodoAndUsuario(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("idUsuario") Long idUsuario
    );

    @Query("SELECT COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true")
    List<Object[]> findVentasByPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT v.usuario.username, COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin AND v.activo = true GROUP BY v.usuario.username ORDER BY SUM(v.total) DESC")
    List<Object[]> findRankingVendedoresPorPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}