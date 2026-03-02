package com.farmacia.repository;

import com.farmacia.enums.EstadoDevolucion;
import com.farmacia.model.Devolucion;
import com.farmacia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    // Para listar todas con todas las relaciones necesarias (venta, vendedor, admin, detalle, medicamento)
    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH d.vendedor vend " +
            "LEFT JOIN FETCH d.admin adm " +
            "LEFT JOIN FETCH d.detalle dt " +
            "LEFT JOIN FETCH dt.medicamento m " +
            "ORDER BY d.fechaSolicitud DESC")
    List<Devolucion> findAllWithAll();

    // Para listar por estado con fetch
    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH d.vendedor vend " +
            "LEFT JOIN FETCH d.admin adm " +
            "LEFT JOIN FETCH d.detalle dt " +
            "LEFT JOIN FETCH dt.medicamento m " +
            "WHERE d.estado = :estado " +
            "ORDER BY d.fechaSolicitud ASC")
    List<Devolucion> findByEstadoWithAll(@Param("estado") EstadoDevolucion estado);

    // Para listar por vendedor con fetch
    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH d.vendedor vend " +
            "LEFT JOIN FETCH d.admin adm " +
            "LEFT JOIN FETCH d.detalle dt " +
            "LEFT JOIN FETCH dt.medicamento m " +
            "WHERE d.vendedor = :vendedor " +
            "ORDER BY d.fechaSolicitud DESC")
    List<Devolucion> findByVendedorWithAll(@Param("vendedor") Usuario vendedor);

    // Para obtener una devolución por ID con todas las relaciones (útil para obtenerPorId)
    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH d.vendedor vend " +
            "LEFT JOIN FETCH d.admin adm " +
            "LEFT JOIN FETCH d.detalle dt " +
            "LEFT JOIN FETCH dt.medicamento m " +
            "WHERE d.id = :id")
    Optional<Devolucion> findByIdWithAll(@Param("id") Long id);

    // Para procesar devolución (necesita venta con detalles)
    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH v.detalles " +
            "LEFT JOIN FETCH d.detalle " +
            "WHERE d.id = :id")
    Optional<Devolucion> findByIdWithVentaAndDetalles(@Param("id") Long id);

    @Query("SELECT d FROM Devolucion d " +
            "JOIN FETCH d.venta v " +
            "JOIN FETCH v.vendedor " +          // vendedor de la venta
            "JOIN FETCH d.vendedor " +          // vendedor que solicitó
            "LEFT JOIN FETCH d.admin " +        // admin que procesó
            "LEFT JOIN FETCH v.cliente " +      // cliente de la venta
            "JOIN FETCH v.detalles det " +      // detalles de la venta
            "LEFT JOIN FETCH det.medicamento " + // medicamento de cada detalle
            "LEFT JOIN FETCH det.lote " +        // lote de cada detalle
            "LEFT JOIN FETCH d.detalle " +       // detalle específico de la devolución (si es parcial)
            "WHERE d.id = :id")
    Optional<Devolucion> findByIdParaPdf(@Param("id") Long id);
}
