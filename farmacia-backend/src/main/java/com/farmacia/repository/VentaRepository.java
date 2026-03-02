package com.farmacia.repository;

import com.farmacia.model.Usuario;
import com.farmacia.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroFactura(String numeroFactura);

    // Para listar ventas por vendedor (no anuladas) con todos los fetch necesarios
    @Query("SELECT v FROM Venta v " +
            "JOIN FETCH v.vendedor " +
            "LEFT JOIN FETCH v.cliente " +
            "LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "LEFT JOIN FETCH d.lote " +
            "WHERE v.vendedor = :vendedor AND v.estado <> 'ANULADA' " +
            "ORDER BY v.fecha DESC")
    List<Venta> findByVendedorWithFetch(@Param("vendedor") Usuario vendedor);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    // Para listar ventas por cliente con todos los fetch
    @Query("SELECT v FROM Venta v " +
            "JOIN FETCH v.vendedor " +
            "LEFT JOIN FETCH v.cliente " +
            "LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "LEFT JOIN FETCH d.lote " +
            "WHERE v.cliente.id = :clienteId AND v.estado <> 'ANULADA' " +
            "ORDER BY v.fecha DESC")
    List<Venta> findByClienteIdWithFetch(@Param("clienteId") Long clienteId);

    @Query("SELECT v.numeroFactura FROM Venta v WHERE v.numeroFactura LIKE :prefijo% ORDER BY v.id DESC")
    List<String> findUltimoNumeroFacturaByPrefijo(@Param("prefijo") String prefijo);

    // Listar todas las ventas no anuladas con todos los fetch (para admin)
    @Query("SELECT v FROM Venta v " +
            "JOIN FETCH v.vendedor " +
            "LEFT JOIN FETCH v.cliente " +
            "LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "LEFT JOIN FETCH d.lote " +
            "WHERE v.estado <> 'ANULADA' " +
            "ORDER BY v.fecha DESC")
    List<Venta> findAllNoAnuladasWithFetch();

    // Obtener una venta con todos los detalles (para uso interno, si es necesario)
    @Query("SELECT v FROM Venta v " +
            "JOIN FETCH v.vendedor " +
            "LEFT JOIN FETCH v.cliente " +
            "LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "LEFT JOIN FETCH d.lote " +
            "WHERE v.id = :id")
    Optional<Venta> findByIdWithAll(@Param("id") Long id);
}