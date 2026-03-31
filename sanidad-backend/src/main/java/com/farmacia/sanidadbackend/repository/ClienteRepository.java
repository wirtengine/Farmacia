package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Cliente;
import com.farmacia.sanidadbackend.model.Venta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByActivoTrue();

    Optional<Cliente> findByIdAndActivoTrue(Long id);

    Optional<Cliente> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :clienteId AND v.activo = true ORDER BY v.fecha DESC")
    List<Venta> findLastPurchasesByCliente(
            @Param("clienteId") Long clienteId,
            Pageable pageable
    );

    @Query("SELECT ld.medicamento.id, ld.medicamento.nombre, SUM(vd.cantidad) as total " +
            "FROM VentaDetalle vd " +
            "JOIN vd.venta v " +
            "JOIN vd.loteDetalle ld " +
            "WHERE v.cliente.id = :clienteId AND v.activo = true " +
            "GROUP BY ld.medicamento.id, ld.medicamento.nombre " +
            "ORDER BY total DESC")
    List<Object[]> findTopProductosByCliente(@Param("clienteId") Long clienteId);
}