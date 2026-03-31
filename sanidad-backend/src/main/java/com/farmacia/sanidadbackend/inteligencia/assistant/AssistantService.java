package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.inteligencia.assistant.dto.AssistantResponse;
import com.farmacia.sanidadbackend.inteligencia.perdidas.PerdidasService;
import com.farmacia.sanidadbackend.inteligencia.perdidas.dto.ResumenPerdidasDTO;
import com.farmacia.sanidadbackend.inteligencia.recommendations.Recommendation;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationPriority;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationService;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DevolucionDetalleRepository devolucionDetalleRepository; // 🔥 nuevo
    private final RackRepository rackRepository; // 🔥 nuevo

    @Autowired
    private AiAssistantService aiAssistantService;

    private List<Intent> intents;

    @PostConstruct
    public void init() {
        intents = new ArrayList<>();

        intents.add(new Intent(
                List.of("ventas", "dia"),
                List.of("hoy", "ingresos"),
                false,
                u -> obtenerVentasDelDia(u)
        ));

        intents.add(new Intent(
                List.of("bajo", "stock"),
                List.of("poco", "escaso"),
                false,
                u -> obtenerProductosBajoStock()
        ));

        intents.add(new Intent(
                List.of("recomendaciones"),
                List.of("sugerencias"),
                true,
                u -> obtenerRecomendaciones()
        ));

        intents.add(new Intent(
                List.of("ultimo", "producto", "vendido"),
                List.of("último", "producto"),
                false,
                u -> obtenerUltimoProductoVendido()
        ));

        intents.add(new Intent(
                List.of("cliente", "mayor", "saldo"),
                List.of("cliente", "saldo"),
                true,
                u -> obtenerClienteMayorSaldo()
        ));
    }

    private record Intent(List<String> keywords, List<String> synonyms, boolean onlyAdmin,
                          Function<Usuario, AssistantResponse> action) {}

    private String normalizar(String texto) {
        return texto.toLowerCase();
    }

    private int calcularScore(Intent intent, String query) {
        int score = 0;
        for (String kw : intent.keywords) {
            if (query.contains(kw)) score += 3;
        }
        for (String syn : intent.synonyms) {
            if (query.contains(syn)) score += 1;
        }
        return score;
    }

    @Transactional
    public AssistantResponse procesarConsulta(String query, Usuario usuario) {
        String queryOriginal = query;
        query = normalizar(query);

        boolean isAdmin = "ADMIN".equals(usuario.getRol());

        Intent bestIntent = null;
        int bestScore = 0;

        for (Intent intent : intents) {
            if (intent.onlyAdmin && !isAdmin) continue;

            int score = calcularScore(intent, query);
            if (score > bestScore) {
                bestScore = score;
                bestIntent = intent;
            }
        }

        // ========== CONSTRUIR CONTEXTO GENERAL CON DATOS REALES ==========
        StringBuilder contexto = new StringBuilder();

        // 1. Ventas del día
        contexto.append("VENTAS DEL DÍA:\n");
        contexto.append(obtenerVentasDelDiaTexto(usuario)).append("\n\n");

        // 2. Ventas del mes
        contexto.append("VENTAS DEL MES:\n");
        contexto.append(obtenerVentasDelMesTexto(usuario)).append("\n\n");

        // 3. Últimos 5 productos vendidos
        contexto.append("ÚLTIMOS 5 PRODUCTOS VENDIDOS (con fecha y hora):\n");
        contexto.append(obtenerUltimosProductosVendidosTexto()).append("\n\n");

        // 4. Productos bajo stock
        contexto.append("PRODUCTOS BAJO STOCK (menos de 10 unidades):\n");
        contexto.append(obtenerProductosBajoStockTexto()).append("\n\n");

        // 5. Recomendaciones (solo admin)
        if (isAdmin) {
            contexto.append("RECOMENDACIONES PENDIENTES:\n");
            contexto.append(obtenerRecomendacionesTexto()).append("\n\n");
        }

        // 6. Resumen de pérdidas (solo admin)
        if (isAdmin) {
            ResumenPerdidasDTO resumen = perdidasService.obtenerResumenPerdidas();
            contexto.append("RESUMEN DE PÉRDIDAS:\n");
            contexto.append("  - Productos vencidos: ").append(resumen.getCantidadProductosVencidos())
                    .append(" (valor perdido: C$ ").append(resumen.getTotalPerdidasVencimiento()).append(")\n");
            contexto.append("  - Productos inmovilizados: ").append(resumen.getCantidadProductosInmoviles())
                    .append(" (valor inmovilizado: C$ ").append(resumen.getTotalInmovilizado()).append(")\n");
            contexto.append("  - Inconsistencias de stock: ").append(resumen.getCantidadInconsistencias()).append("\n\n");
        }

        // 7. Cliente con mayor saldo (solo admin)
        if (isAdmin) {
            contexto.append("CLIENTE CON MAYOR SALDO:\n");
            contexto.append(obtenerClienteMayorSaldoTexto()).append("\n\n");
        }

        // 8. Empleados y vendedores
        contexto.append("EMPLEADOS:\n");
        contexto.append(obtenerEmpleadosTexto()).append("\n\n");

        // 9. Últimas devoluciones aprobadas
        contexto.append("ÚLTIMAS 5 DEVOLUCIONES APROBADAS (más recientes):\n");
        contexto.append(obtenerUltimasDevolucionesTexto()).append("\n\n");

        // 10. Ranking de vendedores por ventas (solo admin)
        if (isAdmin) {
            contexto.append("RANKING DE VENDEDORES POR VENTAS (total histórico):\n");
            contexto.append(obtenerRankingVendedoresTexto()).append("\n\n");
        }

        // 11. Producto con más devoluciones (solo admin)
        if (isAdmin) {
            contexto.append("PRODUCTO CON MÁS DEVOLUCIONES (histórico):\n");
            contexto.append(obtenerProductoMasDevueltoTexto()).append("\n\n");
        }

        // 12. Cantidad de estantes
        contexto.append("CANTIDAD DE ESTANTES (racks activos):\n");
        contexto.append(obtenerCantidadEstantesTexto()).append("\n\n");

        // 13. Producto con mayor crecimiento de ventas (últimos 30 días vs período anterior)
        if (isAdmin) {
            contexto.append("PRODUCTO CON MAYOR CRECIMIENTO DE VENTAS (últimos 30 días vs 30 días anteriores):\n");
            contexto.append(obtenerProductoMayorCrecimientoTexto()).append("\n\n");
        }

        // 14. Respuesta específica si hay intent de alta confianza
        if (bestIntent != null && bestScore >= 2) {
            AssistantResponse intentResponse = bestIntent.action.apply(usuario);
            contexto.append("RESPUESTA ESPECÍFICA A TU CONSULTA:\n");
            contexto.append(intentResponse.getAnswer()).append("\n\n");
        }

        // Llamada a la IA con el contexto completo
        String aiAnswer = aiAssistantService.consultarIAConContexto(
                queryOriginal,
                usuario,
                contexto.toString()
        );

        return AssistantResponse.builder()
                .answer(aiAnswer)
                .build();
    }

    // ================== MÉTODOS CON DATOS REALES ==================

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

        // Calcular crecimiento
        Map<Long, Double> crecimiento = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : actualMap.entrySet()) {
            Long medId = entry.getKey();
            int actual = entry.getValue();
            int anterior = anteriorMap.getOrDefault(medId, 0);
            if (anterior > 0) {
                crecimiento.put(medId, (double) actual / anterior);
            } else if (actual > 0) {
                crecimiento.put(medId, Double.MAX_VALUE); // nuevo producto con ventas
            }
        }

        // Obtener el de mayor crecimiento
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

    // ================== MÉTODOS PARA INTENTS ==================
    private AssistantResponse obtenerVentasDelDia(Usuario usuario) {
        return AssistantResponse.builder()
                .answer(obtenerVentasDelDiaTexto(usuario))
                .build();
    }

    private AssistantResponse obtenerProductosBajoStock() {
        return AssistantResponse.builder()
                .answer(obtenerProductosBajoStockTexto())
                .build();
    }

    private AssistantResponse obtenerRecomendaciones() {
        return AssistantResponse.builder()
                .answer(obtenerRecomendacionesTexto())
                .build();
    }

    private AssistantResponse obtenerUltimoProductoVendido() {
        String ultimos = obtenerUltimosProductosVendidosTexto();
        if (ultimos.contains("No hay ventas")) {
            return AssistantResponse.builder().answer(ultimos).build();
        }
        // Extraer el primer producto de la primera venta
        String[] lineas = ultimos.split("\n");
        for (String linea : lineas) {
            if (linea.trim().startsWith("-")) {
                return AssistantResponse.builder().answer(linea.trim()).build();
            }
        }
        return AssistantResponse.builder().answer("No se pudo determinar el último producto vendido.").build();
    }

    private AssistantResponse obtenerClienteMayorSaldo() {
        return AssistantResponse.builder()
                .answer(obtenerClienteMayorSaldoTexto())
                .build();
    }
}