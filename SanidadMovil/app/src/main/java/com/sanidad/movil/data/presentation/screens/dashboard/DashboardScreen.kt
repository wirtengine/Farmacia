package com.sanidad.movil.presentation.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.sanidad.movil.data.remote.dto.DashboardResponseDTO
import java.text.NumberFormat
import java.util.*

// ── Paleta unificada con PerdidasScreen ──────────────────────────────────────
private val Indigo950  = Color(0xFF1E1B4B)
private val Indigo600  = Color(0xFF4F46E5)
private val Indigo100  = Color(0xFFE0E7FF)
private val Emerald600 = Color(0xFF059669)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald50  = Color(0xFFECFDF5)
private val Rose600    = Color(0xFFDC2626)
private val Rose100    = Color(0xFFFEE2E2)
private val Rose50     = Color(0xFFFFF1F2)
private val Amber600   = Color(0xFFD97706)
private val Amber50    = Color(0xFFFFFBEB)
private val Slate900   = Color(0xFF0F172A)
private val Slate700   = Color(0xFF334155)
private val Slate500   = Color(0xFF64748B)
private val Slate400   = Color(0xFF94A3B8)
private val Slate200   = Color(0xFFE2E8F0)
private val Slate100   = Color(0xFFF1F5F9)
private val Slate50    = Color(0xFFF8FAFC)
private val White      = Color.White

// ── Pantalla principal ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateAlerts: () -> Unit = {},
    onNavigateRecommendations: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val wideMode = LocalConfiguration.current.screenWidthDp > 600

    Scaffold(containerColor = Slate50) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.dashboard == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Emerald600,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                state.error != null -> {
                    DashboardError(state.error!!) { viewModel.loadData() }
                }
                state.dashboard == null -> {
                    DashboardError("No hay datos disponibles") { viewModel.loadData() }
                }
                else -> {
                    DashboardContent(
                        data = state.dashboard!!,
                        pendingAlerts = state.pendingAlerts,
                        pendingRecs = state.pendingRecs,
                        lastUpdated = state.lastUpdated,
                        wideMode = wideMode,
                        onNavigateAlerts = onNavigateAlerts,
                        onNavigateRecommendations = onNavigateRecommendations,
                        onRefresh = { viewModel.loadData() },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

// ── Estado de error ───────────────────────────────────────────────────────────
@Composable
private fun DashboardError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Rose50),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = Rose600, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(message, fontSize = 15.sp, color = Slate700, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald600)
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Reintentar", color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ── Contenido principal ───────────────────────────────────────────────────────
@Composable
private fun DashboardContent(
    data: DashboardResponseDTO,
    pendingAlerts: Int,
    pendingRecs: Int,
    lastUpdated: String?,
    wideMode: Boolean,
    onNavigateAlerts: () -> Unit,
    onNavigateRecommendations: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ──
        item {
            DashboardHeader(
                lastUpdated = lastUpdated,
                onRefresh = onRefresh,
                onLogout = onLogout
            )
        }

        // ── KPIs ──
        item {
            if (wideMode) {
                KpiRowWide(
                    ventasHoy = data.ventasDelDia?.totalVentas ?: 0.0,
                    ventasMes = data.ventasMesActual ?: 0.0,
                    facturasHoy = data.ventasDelDia?.cantidadVentas ?: 0,
                    pendingAlerts = pendingAlerts,
                    pendingRecs = pendingRecs,
                    onAlertsClick = onNavigateAlerts,
                    onRecsClick = onNavigateRecommendations
                )
            } else {
                KpiGridNarrow(
                    ventasHoy = data.ventasDelDia?.totalVentas ?: 0.0,
                    ventasMes = data.ventasMesActual ?: 0.0,
                    facturasHoy = data.ventasDelDia?.cantidadVentas ?: 0,
                    pendingAlerts = pendingAlerts,
                    pendingRecs = pendingRecs,
                    onAlertsClick = onNavigateAlerts,
                    onRecsClick = onNavigateRecommendations
                )
            }
        }

        // ── Gráficos ──
        item {
            SectionLabel("Análisis de ventas")
        }
        item {
            if (wideMode) {
                ChartsGridWide(data)
            } else {
                ChartsListNarrow(data)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    lastUpdated: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo950,
                    letterSpacing = (-0.5).sp
                )
                if (lastUpdated != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Actualizado $lastUpdated",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Salir — mínimo, texto discreto
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onLogout() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Salir",
                        fontSize = 13.sp,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Botón Refresh — ícono pequeño en caja verde
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Emerald50)
                        .clickable { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Recargar",
                        tint = Emerald600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Divider(color = Slate200)
    }
}

// ── Etiqueta de sección ───────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Slate400,
        letterSpacing = 0.8.sp
    )
}

// ── KPI Grid: modo vertical (2 × 2 + fila inferior) ──────────────────────────
@Composable
private fun KpiGridNarrow(
    ventasHoy: Double, ventasMes: Double,
    facturasHoy: Int, pendingAlerts: Int, pendingRecs: Int,
    onAlertsClick: () -> Unit, onRecsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fila 1 — Ventas hoy y mes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricKpi(
                label = "Ventas hoy",
                value = formatCurrency(ventasHoy),
                accentColor = Emerald600,
                bgColor = Emerald50,
                modifier = Modifier.weight(1f)
            )
            MetricKpi(
                label = "Ventas del mes",
                value = formatCurrency(ventasMes),
                accentColor = Indigo600,
                bgColor = Indigo100.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
        }
        // Fila 2 — Facturas + Alertas + Recs
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricKpi(
                label = "Facturas hoy",
                value = "$facturasHoy",
                accentColor = Slate700,
                bgColor = Slate100,
                modifier = Modifier.weight(1f)
            )
            ActionKpi(
                label = "Alertas",
                count = pendingAlerts,
                sub = "Pendientes",
                accentColor = Rose600,
                bgColor = Rose50,
                icon = Icons.Default.Notifications,
                onClick = onAlertsClick,
                modifier = Modifier.weight(1f)
            )
            ActionKpi(
                label = "Sugerencias",
                count = pendingRecs,
                sub = "De optimización",
                accentColor = Amber600,
                bgColor = Amber50,
                icon = Icons.Default.Lightbulb,
                onClick = onRecsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Divider(color = Slate200)
}

// ── KPI Row: modo horizontal (5 en fila) ──────────────────────────────────────
@Composable
private fun KpiRowWide(
    ventasHoy: Double, ventasMes: Double,
    facturasHoy: Int, pendingAlerts: Int, pendingRecs: Int,
    onAlertsClick: () -> Unit, onRecsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricKpi("Ventas hoy", formatCurrency(ventasHoy), Emerald600, Emerald50, Modifier.weight(1f))
        MetricKpi("Ventas del mes", formatCurrency(ventasMes), Indigo600, Indigo100.copy(alpha = 0.4f), Modifier.weight(1f))
        MetricKpi("Facturas hoy", "$facturasHoy", Slate700, Slate100, Modifier.weight(0.7f))
        ActionKpi("Alertas", pendingAlerts, "Pendientes", Rose600, Rose50, Icons.Default.Notifications, onAlertsClick, Modifier.weight(0.8f))
        ActionKpi("Sugerencias", pendingRecs, "Stock", Amber600, Amber50, Icons.Default.Lightbulb, onRecsClick, Modifier.weight(0.8f))
    }
    Divider(color = Slate200)
}

// ── KPI de métrica simple ─────────────────────────────────────────────────────
@Composable
private fun MetricKpi(
    label: String,
    value: String,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.7.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

// ── KPI de acción (con tap) ───────────────────────────────────────────────────
@Composable
private fun ActionKpi(
    label: String,
    count: Int,
    sub: String,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.7.sp
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                letterSpacing = (-0.5).sp
            )
            Text(sub, fontSize = 10.sp, color = accentColor.copy(alpha = 0.7f))
        }
    }
}

// ── Gráficos: modo vertical (apilados) ───────────────────────────────────────
@Composable
private fun ChartsListNarrow(data: DashboardResponseDTO) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChartCard("Vendedores — ventas totales") {
            val entries = data.rankingVendedores?.map { it.totalVentas.toFloat() } ?: emptyList()
            if (entries.isNotEmpty()) {
                Chart(
                    chart = columnChart(),
                    model = entryModelOf(*entries.toTypedArray()),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis()
                )
            } else ChartEmpty()
        }
        ChartCard("Tendencia — mes anterior vs actual") {
            val entries = arrayOf(
                data.ventasMesAnterior?.toFloat() ?: 0f,
                data.ventasMesActual?.toFloat() ?: 0f
            )
            Chart(
                chart = lineChart(),
                model = entryModelOf(*entries),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            )
        }
        ChartCard("Productos más rentables") {
            val pieData = data.productosMasRentables
                ?.map { it.nombre to it.ingresos.toFloat() } ?: emptyList()
            if (pieData.isNotEmpty()) PieChart(pieData) else ChartEmpty()
        }
        ChartCard("Clientes frecuentes") {
            ChartEmpty("Próximamente")
        }
    }
}

// ── Gráficos: modo horizontal (2 × 2 grid) ───────────────────────────────────
@Composable
private fun ChartsGridWide(data: DashboardResponseDTO) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChartCard("Vendedores — ventas totales", modifier = Modifier.weight(1f)) {
                val entries = data.rankingVendedores?.map { it.totalVentas.toFloat() } ?: emptyList()
                if (entries.isNotEmpty()) {
                    Chart(
                        chart = columnChart(),
                        model = entryModelOf(*entries.toTypedArray()),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis()
                    )
                } else ChartEmpty()
            }
            ChartCard("Tendencia — mes anterior vs actual", modifier = Modifier.weight(1f)) {
                val entries = arrayOf(
                    data.ventasMesAnterior?.toFloat() ?: 0f,
                    data.ventasMesActual?.toFloat() ?: 0f
                )
                Chart(
                    chart = lineChart(),
                    model = entryModelOf(*entries),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis()
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChartCard("Productos más rentables", modifier = Modifier.weight(1f)) {
                val pieData = data.productosMasRentables
                    ?.map { it.nombre to it.ingresos.toFloat() } ?: emptyList()
                if (pieData.isNotEmpty()) PieChart(pieData) else ChartEmpty()
            }
            ChartCard("Clientes frecuentes", modifier = Modifier.weight(1f)) {
                ChartEmpty("Próximamente")
            }
        }
    }
}

// ── Chart Card contenedor ────────────────────────────────────────────────────
@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.height(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp).fillMaxSize()) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                letterSpacing = (-0.1).sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

// ── Pie chart simple ──────────────────────────────────────────────────────────
private val PIE_COLORS = listOf(
    Color(0xFF059669), Color(0xFF4F46E5), Color(0xFFD97706),
    Color(0xFFDC2626), Color(0xFF0EA5E9)
)

@Composable
private fun PieChart(data: List<Pair<String, Float>>) {
    val total = data.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: return
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            var start = -90f
            data.forEachIndexed { i, p ->
                val sweep = (p.second / total) * 360f
                drawArc(
                    color = PIE_COLORS[i % PIE_COLORS.size],
                    startAngle = start, sweepAngle = sweep,
                    useCenter = true, size = Size(size.width, size.height)
                )
                start += sweep
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            data.forEachIndexed { i, p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PIE_COLORS[i % PIE_COLORS.size])
                    )
                    Text(
                        p.first,
                        fontSize = 10.sp,
                        color = Slate500,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        formatCurrency(p.second.toDouble()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900
                    )
                }
            }
        }
    }
}

// ── Estado vacío de gráfico ───────────────────────────────────────────────────
@Composable
private fun ChartEmpty(message: String = "Sin datos disponibles") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.BarChart,
            null,
            tint = Slate200,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 12.sp, color = Slate400)
    }
}

// ── Formato moneda ────────────────────────────────────────────────────────────
private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)