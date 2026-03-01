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

    List<Venta> findByVendedorOrderByFechaDesc(Usuario vendedor);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :clienteId ORDER BY v.fecha DESC")
    List<Venta> findByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT v.numeroFactura FROM Venta v WHERE v.numeroFactura LIKE :prefijo% ORDER BY v.id DESC")
    List<String> findUltimoNumeroFacturaByPrefijo(@Param("prefijo") String prefijo);
}