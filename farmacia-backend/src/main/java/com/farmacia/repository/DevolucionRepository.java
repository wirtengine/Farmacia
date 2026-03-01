package com.farmacia.repository;

import com.farmacia.enums.EstadoDevolucion;
import com.farmacia.model.Devolucion;
import com.farmacia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    // Listar devoluciones pendientes ordenadas por fecha de solicitud ascendente
    List<Devolucion> findByEstadoOrderByFechaSolicitudAsc(EstadoDevolucion estado);

    // Listar devoluciones de un vendedor específico ordenadas por fecha descendente
    List<Devolucion> findByVendedorOrderByFechaSolicitudDesc(Usuario vendedor);

    // Listar todas las devoluciones ordenadas por fecha descendente (para admin)
    List<Devolucion> findAllByOrderByFechaSolicitudDesc();
}