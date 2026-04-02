package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.inteligencia.assistant.dto.AssistantResponse;
import com.farmacia.sanidadbackend.inteligencia.perdidas.PerdidasService;
import com.farmacia.sanidadbackend.inteligencia.perdidas.dto.ResumenPerdidasDTO;
import com.farmacia.sanidadbackend.inteligencia.recommendations.Recommendation;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationPriority;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationService;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import com.farmacia.sanidadbackend.service.PrediccionService;
import com.farmacia.sanidadbackend.service.ReporteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final VentaRepository ventaRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final LoteRepository loteRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final PerdidasService perdidasService;
    private final RecommendationService recommendationService;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DevolucionRepository devolucionRepository;
    private final DevolucionDetalleRepository devolucionDetalleRepository;
    private final RackRepository rackRepository;
    private final ReporteService reporteService;
    private final PrediccionService prediccionService;

    // ================== MÉTODOS PARA CONSTRUIR CONTEXTO (usado por AiAssistantService) ==================

    public String construirContexto(Usuario usuario) {
        StringBuilder contexto = new StringBuilder();
        boolean isAdmin = "ADMIN".equals(usuario.getRol());

        contexto.append("VENTAS DEL DÍA:\n");
        contexto.append(obtenerVentasDelDiaTexto(usuario)).append("\n\n");

        contexto.append("VENTAS DEL MES:\n");
        contexto.append(obtenerVentasDelMesTexto(usuario)).append("\n\n");

        contexto.append("ÚLTIMOS 5 PRODUCTOS VENDIDOS:\n");
        contexto.append(obtenerUltimosProductosVendidosTexto()).append("\n\n");

        contexto.append("PRODUCTOS BAJO STOCK (menos de 10 unidades):\n");
        contexto.append(obtenerProductosBajoStockTexto()).append("\n\n");

        if (isAdmin) {
            contexto.append("RECOMENDACIONES PENDIENTES:\n");
            contexto.append(obtenerRecomendacionesTexto()).append("\n\n");

            ResumenPerdidasDTO resumen = perdidasService.obtenerResumenPerdidas();
            contexto.append("RESUMEN DE PÉRDIDAS:\n");
            contexto.append("  - Productos vencidos: ").append(resumen.getCantidadProductosVencidos())
                    .append(" (valor perdido: C$ ").append(resumen.getTotalPerdidasVencimiento()).append(")\n");
            contexto.append("  - Productos inmovilizados: ").append(resumen.getCantidadProductosInmoviles())
                    .append(" (valor inmovilizado: C$ ").append(resumen.getTotalInmovilizado()).append(")\n");
            contexto.append("  - Inconsistencias de stock: ").append(resumen.getCantidadInconsistencias()).append("\n\n");

            contexto.append("CLIENTE CON MAYOR SALDO:\n");
            contexto.append(obtenerClienteMayorSaldoTexto()).append("\n\n");

            contexto.append("RANKING DE VENDEDORES POR VENTAS (total histórico):\n");
            contexto.append(obtenerRankingVendedoresTexto()).append("\n\n");

            contexto.append("PRODUCTO CON MÁS DEVOLUCIONES (histórico):\n");
            contexto.append(obtenerProductoMasDevueltoTexto()).append("\n\n");

            contexto.append("PRODUCTO CON MAYOR CRECIMIENTO DE VENTAS (últimos 30 días vs período anterior):\n");
            contexto.append(obtenerProductoMayorCrecimientoTexto()).append("\n\n");
        }

        contexto.append("EMPLEADOS:\n");
        contexto.append(obtenerEmpleadosTexto()).append("\n\n");

        contexto.append("ÚLTIMAS 5 DEVOLUCIONES APROBADAS:\n");
        contexto.append(obtenerUltimasDevolucionesTexto()).append("\n\n");

        contexto.append("CANTIDAD DE ESTANTES (racks activos):\n");
        contexto.append(obtenerCantidadEstantesTexto()).append("\n\n");

        return contexto.toString();
    }

    // ================== MÉTODOS PRIVADOS DE OBTENCIÓN DE TEXTO ==================

    private String obtenerVentasDelDiaTexto(Usuario usuario) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);
        boolean esAdmin = "ADMIN".equals(usuario.getRol());

        List<Object[]> ventasDia;
        if (esAdmin) {
            ventasDia = ventaRepository.findVentasDelDia(inicioDia, finDia);
        } else {
            ventasDia = ventaRepository.findVentasDelDiaByUsuario(inicioDia, finDia, usuario.getId());
        }

        if (ventasDia.isEmpty() || ventasDia.get(0) == null) {
            return "No se registraron ventas hoy.";
        } else {
            Object[] row = ventasDia.get(0);
            long cantidad = ((Number) row[0]).longValue();
            BigDecimal total = (BigDecimal) row[1];
            return String.format("Total ventas: C$ %.2f ( %d ventas)", total, cantidad);
        }
    }

    private String obtenerVentasDelMesTexto(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.withDayOfMonth(hoy.lengthOfMonth()).atTime(LocalTime.MAX);
        boolean esAdmin = "ADMIN".equals(usuario.getRol());

        BigDecimal total;
        if (esAdmin) {
            total = ventaRepository.sumVentasByPeriodo(inicioMes, finMes);
        } else {
            total = ventaRepository.sumVentasByPeriodoAndUsuario(inicioMes, finMes, usuario.getId());
        }
        return total != null ? String.format("C$ %.2f", total) : "C$ 0.00";
    }

    private String obtenerUltimosProductosVendidosTexto() {
        List<Venta> ultimasVentas = ventaRepository.findTop5ByActivoTrueOrderByFechaDesc();
        if (ultimasVentas.isEmpty()) {
            return "No hay ventas registradas.";
        }

        StringBuilder sb = new StringBuilder();
        for (Venta venta : ultimasVentas) {
            sb.append("Venta ").append(venta.getNumeroFactura())
                    .append(" (").append(venta.getFecha().toLocalDate()).append(" ").append(venta.getFecha().toLocalTime()).append("):\n");
            for (VentaDetalle detalle : venta.getDetalles()) {
                sb.append("  - ").append(detalle.getLoteDetalle().getMedicamento().getNombre())
                        .append(": ").append(detalle.getCantidad())
                        .append(" unidades (C$ ").append(detalle.getPrecioUnitario()).append(" c/u)\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String obtenerProductosBajoStockTexto() {
        List<Object[]> bajoStock = loteDetalleRepository.findProductosBajoStock();
        if (bajoStock.isEmpty()) {
            return "No hay productos con stock bajo (menos de 10 unidades).";
        }
        StringBuilder sb = new StringBuilder();
        for (Object[] row : bajoStock) {
            String nombre = (String) row[0];
            int stock = ((Number) row[1]).intValue();
            sb.append("- ").append(nombre).append(": ").append(stock).append(" unidades\n");
        }
        return sb.toString();
    }

    private String obtenerRecomendacionesTexto() {
        List<Recommendation> lista = recommendationService.obtenerRecomendacionesPendientes();
        if (lista.isEmpty()) {
            return "No hay recomendaciones pendientes.";
        }
        Map<RecommendationPriority, List<Recommendation>> agrupadas =
                lista.stream().collect(Collectors.groupingBy(Recommendation::getPriority));
        StringBuilder sb = new StringBuilder();
        for (RecommendationPriority p : RecommendationPriority.values()) {
            if (!agrupadas.containsKey(p)) continue;
            sb.append(p.name()).append(":\n");
            for (Recommendation r : agrupadas.get(p)) {
                sb.append("- ").append(r.getTitle()).append("\n");
            }
        }
        return sb.toString();
    }

    private String obtenerClienteMayorSaldoTexto() {
        List<Cliente> clientes = clienteRepository.findAllByActivoTrue();
        if (clientes.isEmpty()) {
            return "No hay clientes registrados.";
        }
        Cliente mayor = clientes.stream()
                .max(Comparator.comparing(Cliente::getSaldo))
                .orElse(null);
        if (mayor == null || mayor.getSaldo() == null || mayor.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            return "Ningún cliente tiene saldo positivo.";
        }
        return String.format("%s (Cédula: %s) tiene un saldo de C$ %.2f",
                mayor.getNombre(), mayor.getCedula(), mayor.getSaldo());
    }

    private String obtenerEmpleadosTexto() {
        long totalEmpleados = usuarioRepository.count();
        long vendedores = usuarioRepository.countByRol(Rol.VENDEDOR);
        long administradores = usuarioRepository.countByRol(Rol.ADMIN);
        return String.format("Total empleados: %d\n  - Vendedores: %d\n  - Administradores: %d",
                totalEmpleados, vendedores, administradores);
    }

    private String obtenerUltimasDevolucionesTexto() {
        List<Devolucion> ultimas = devolucionRepository.findTop5ByEstadoOrderByFechaSolicitudDesc(EstadoDevolucion.APROBADA);
        if (ultimas.isEmpty()) {
            return "No hay devoluciones aprobadas registradas.";
        }
        StringBuilder sb = new StringBuilder();
        for (Devolucion d : ultimas) {
            sb.append("Devolución ").append(d.getNumeroDevolucion())
                    .append(" - Fecha: ").append(d.getFechaSolicitud().toLocalDate())
                    .append(" - Total reembolsado: C$ ").append(d.getTotalDevuelto())
                    .append(" - Motivo: ").append(d.getMotivo()).append("\n");
            sb.append("  Productos devueltos:\n");
            for (DevolucionDetalle det : d.getDetalles()) {
                sb.append("    - ").append(det.getLoteDetalle().getMedicamento().getNombre())
                        .append(": ").append(det.getCantidadDevuelta()).append(" unidades\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String obtenerRankingVendedoresTexto() {
        List<Object[]> ranking = ventaRepository.findRankingVendedores();
        if (ranking.isEmpty()) {
            return "No hay datos de ventas de vendedores.";
        }
        StringBuilder sb = new StringBuilder();
        for (Object[] row : ranking) {
            String username = (String) row[0];
            long cantidad = ((Number) row[1]).longValue();
            BigDecimal total = (BigDecimal) row[2];
            sb.append("- ").append(username).append(": ").append(cantidad).append(" ventas, C$ ")
                    .append(total).append("\n");
        }
        return sb.toString();
    }

    private String obtenerProductoMasDevueltoTexto() {
        List<Object[]> devueltos = devolucionDetalleRepository.findTotalDevueltoPorMedicamento();
        if (devueltos.isEmpty()) {
            return "No hay devoluciones registradas.";
        }
        Object[] top = devueltos.get(0);
        String nombre = (String) top[1];
        long cantidad = ((Number) top[2]).longValue();
        return String.format("%s con %d unidades devueltas.", nombre, cantidad);
    }

    private String obtenerCantidadEstantesTexto() {
        long total = rackRepository.countByActivoTrue();
        return String.format("Total de estantes activos: %d", total);
    }

    private String obtenerProductoMayorCrecimientoTexto() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioPeriodoActual = ahora.minusDays(30);
        LocalDateTime finPeriodoActual = ahora;
        LocalDateTime inicioPeriodoAnterior = inicioPeriodoActual.minusDays(30);
        LocalDateTime finPeriodoAnterior = inicioPeriodoActual.minusNanos(1);

        List<Object[]> ventasActual = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(inicioPeriodoActual, finPeriodoActual);
        List<Object[]> ventasAnterior = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(inicioPeriodoAnterior, finPeriodoAnterior);

        Map<Long, Integer> actualMap = ventasActual.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));
        Map<Long, Integer> anteriorMap = ventasAnterior.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));

        Map<Long, Double> crecimiento = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : actualMap.entrySet()) {
            Long medId = entry.getKey();
            int actual = entry.getValue();
            int anterior = anteriorMap.getOrDefault(medId, 0);
            if (anterior > 0) {
                crecimiento.put(medId, (double) actual / anterior);
            } else if (actual > 0) {
                crecimiento.put(medId, Double.MAX_VALUE);
            }
        }

        Long topMedId = crecimiento.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topMedId == null) {
            return "No hay datos suficientes para determinar crecimiento.";
        }

        Optional<Medicamento> med = medicamentoRepository.findById(topMedId);
        if (med.isEmpty()) return "No se encontró el medicamento.";
        int actual = actualMap.getOrDefault(topMedId, 0);
        int anterior = anteriorMap.getOrDefault(topMedId, 0);
        double factor = anterior == 0 ? actual : (double) actual / anterior;
        return String.format("%s: vendió %d unidades en los últimos 30 días, %d en los 30 días anteriores (crecimiento x%.1f).",
                med.get().getNombre(), actual, anterior, factor);
    }

    // ================== MÉTODOS PÚBLICOS PARA FUNCIONES (FUNCTION CALLING) ==================

    public Map<String, Object> obtenerVentasPorPeriodo(Map<String, Object> args, Usuario usuario) {
        LocalDate inicio = LocalDate.parse((String) args.get("fechaInicio"));
        LocalDate fin = LocalDate.parse((String) args.get("fechaFin"));
        Integer idVendedor = args.containsKey("idUsuario") ? ((Number) args.get("idUsuario")).intValue() : null;

        if (idVendedor != null && !"ADMIN".equals(usuario.getRol())) {
            if (!idVendedor.equals(usuario.getId())) {
                return Map.of("error", "No tienes permiso para ver ventas de otro vendedor.");
            }
        }

        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = fin.atTime(LocalTime.MAX);

        List<Object[]> ventas;
        if (idVendedor != null) {
            Long idVendedorLong = idVendedor.longValue();
            ventas = ventaRepository.findVentasByPeriodoAndUsuario(start, end, idVendedorLong);
        } else {
            ventas = ventaRepository.findVentasByPeriodo(start, end);
        }

        if (ventas.isEmpty() || ventas.get(0) == null) {
            return Map.of("totalVentas", BigDecimal.ZERO, "cantidadVentas", 0);
        }

        Object[] row = ventas.get(0);
        long cantidad = ((Number) row[0]).longValue();
        BigDecimal total = (BigDecimal) row[1];
        return Map.of("totalVentas", total, "cantidadVentas", cantidad);
    }

    public List<Map<String, Object>> obtenerProductosBajoStock(Map<String, Object> args, Usuario usuario) {
        int umbral = args.containsKey("umbral") ? ((Number) args.get("umbral")).intValue() : 10;
        List<Object[]> productos = loteDetalleRepository.findProductosBajoStockConUmbral(umbral);
        return productos.stream()
                .map(row -> Map.of("nombre", row[0], "stock", row[1]))
                .collect(Collectors.toList());
    }

    public Map<String, Object> obtenerDetallesCliente(Map<String, Object> args, Usuario usuario) {
        Optional<Cliente> clienteOpt = Optional.empty();
        if (args.containsKey("id")) {
            Long id = ((Number) args.get("id")).longValue();
            clienteOpt = clienteRepository.findById(id);
        } else if (args.containsKey("cedula")) {
            String cedula = (String) args.get("cedula");
            clienteOpt = clienteRepository.findByCedula(cedula);
        } else {
            return Map.of("error", "Debe proporcionar id o cédula del cliente.");
        }

        if (clienteOpt.isEmpty()) {
            return Map.of("error", "Cliente no encontrado.");
        }

        Cliente c = clienteOpt.get();
        return Map.of(
                "id", c.getId(),
                "nombre", c.getNombre(),
                "cedula", c.getCedula(),
                "telefono", c.getTelefono(),
                "email", c.getEmail(),
                "saldo", c.getSaldo(),
                "activo", c.getActivo()
        );
    }

    public Map<String, Object> obtenerStockActual(Map<String, Object> args, Usuario usuario) {
        Optional<Medicamento> medOpt = Optional.empty();
        if (args.containsKey("id")) {
            Long id = ((Number) args.get("id")).longValue();
            medOpt = medicamentoRepository.findById(id);
        } else if (args.containsKey("nombre")) {
            String nombre = (String) args.get("nombre");
            medOpt = medicamentoRepository.findByNombreIgnoreCaseAndActivoTrue(nombre);
        } else {
            return Map.of("error", "Debe proporcionar id o nombre del medicamento.");
        }

        if (medOpt.isEmpty()) {
            return Map.of("error", "Medicamento no encontrado.");
        }

        Medicamento m = medOpt.get();
        int stockTotal = loteDetalleRepository.sumStockByMedicamento(m.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("id", m.getId());
        result.put("nombre", m.getNombre());
        result.put("stockTotal", stockTotal);
        result.put("presentacion", m.getPresentacion());
        return result;
    }

    public List<Map<String, Object>> sugerirReorden(Map<String, Object> args, Usuario usuario) {
        int umbralDias = args.containsKey("umbralDias") ? ((Number) args.get("umbralDias")).intValue() : 30;
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(umbralDias);
        LocalDateTime inicioDia = inicio.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        List<Object[]> resultados = loteDetalleRepository.sugerirReorden(inicioDia, finDia);
        List<Map<String, Object>> sugerencias = new ArrayList<>();
        for (Object[] row : resultados) {
            String nombre = (String) row[1];
            int stock = ((Number) row[2]).intValue();
            int ventas = ((Number) row[3]).intValue();
            double diasInventario = ventas == 0 ? Double.POSITIVE_INFINITY : (double) stock / (ventas / (double) umbralDias);
            if (diasInventario < 15) {
                Map<String, Object> sug = new HashMap<>();
                sug.put("nombre", nombre);
                sug.put("stock", stock);
                sug.put("ventasUltimosDias", ventas);
                sug.put("diasInventarioEstimados", diasInventario);
                sug.put("sugerencia", "Reordenar urgentemente, stock bajo para la demanda.");
                sugerencias.add(sug);
            }
        }
        return sugerencias;
    }

    public List<Map<String, Object>> obtenerRankingVendedores(Map<String, Object> args, Usuario usuario) {
        if (!"ADMIN".equals(usuario.getRol())) {
            return List.of(Map.of("error", "No tienes permiso para ver el ranking de vendedores."));
        }

        String periodo = args.containsKey("periodo") ? (String) args.get("periodo") : "historico";
        LocalDateTime inicio = null;
        if ("mensual".equals(periodo)) {
            inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        } else if ("anual".equals(periodo)) {
            inicio = LocalDate.now().withDayOfYear(1).atStartOfDay();
        }

        List<Object[]> ranking;
        if (inicio != null) {
            ranking = ventaRepository.findRankingVendedoresPorPeriodo(inicio, LocalDateTime.now());
        } else {
            ranking = ventaRepository.findRankingVendedores();
        }

        return ranking.stream()
                .map(row -> Map.of("usuario", row[0], "cantidadVentas", row[1], "totalVentas", row[2]))
                .collect(Collectors.toList());
    }

    public Map<String, Object> generarReporteVentas(Map<String, Object> args, Usuario usuario) {
        if (!"ADMIN".equals(usuario.getRol())) {
            return Map.of("error", "Solo administradores pueden generar reportes.");
        }

        LocalDate inicio = LocalDate.parse((String) args.get("fechaInicio"));
        LocalDate fin = LocalDate.parse((String) args.get("fechaFin"));
        String formato = ((String) args.get("formato")).toUpperCase();

        LocalDateTime inicioDT = inicio.atStartOfDay();
        LocalDateTime finDT = fin.atTime(LocalTime.MAX);

        try {
            String ruta;
            if (formato.equals("EXCEL")) {
                ruta = reporteService.generarExcelVentas(inicioDT, finDT);
            } else if (formato.equals("PDF")) {
                ruta = reporteService.generarPdfVentas(inicioDT, finDT);
            } else {
                return Map.of("error", "Formato no soportado. Use PDF o EXCEL.");
            }
            return Map.of("mensaje", "Reporte generado exitosamente", "archivo", ruta);
        } catch (IOException e) {
            return Map.of("error", "Error al generar reporte: " + e.getMessage());
        }
    }

    public Map<String, Object> predecirVentasMedicamento(Map<String, Object> args, Usuario usuario) {
        if (!"ADMIN".equals(usuario.getRol())) {
            return Map.of("error", "Solo administradores pueden acceder a predicciones.");
        }

        Long medicamentoId;
        if (args.containsKey("medicamentoId")) {
            medicamentoId = ((Number) args.get("medicamentoId")).longValue();
        } else if (args.containsKey("nombre")) {
            String nombre = (String) args.get("nombre");
            Optional<Medicamento> med = medicamentoRepository.findByNombreIgnoreCaseAndActivoTrue(nombre);
            if (med.isEmpty()) {
                return Map.of("error", "Medicamento no encontrado.");
            }
            medicamentoId = med.get().getId();
        } else {
            return Map.of("error", "Debe proporcionar medicamentoId o nombre.");
        }

        int diasHorizonte = args.containsKey("dias") ? ((Number) args.get("dias")).intValue() : 7;
        return prediccionService.predecirVentas(medicamentoId, diasHorizonte);
    }
}