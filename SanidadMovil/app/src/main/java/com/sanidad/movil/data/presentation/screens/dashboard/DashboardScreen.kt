package com.sanidad.movil.presentation.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.util.*

// Paleta de diseño
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val IndigoLight = Color(0xFFE0E7FF)
private val IndigoText = Color(0xFF4338CA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateAlerts: () -> Unit = {},
    onNavigateRecommendations: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(containerColor = Slate50) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading && state.dashboard == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Success)
                    }
                }
                state.error != null -> {
                    EmptyErrorState(state.error!!) { viewModel.loadData() }
                }
                state.dashboard == null -> {
                    EmptyErrorState("No hay datos disponibles") { viewModel.loadData() }
                }
                else -> {
                    DashboardContent(
                        data = state.dashboard!!,
                        pendingAlerts = state.pendingAlerts.toInt(),
                        pendingRecs = state.pendingRecs.toInt(),
                        lastUpdated = state.lastUpdated,
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

@Composable
private fun EmptyErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = Danger)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Success)) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun DashboardContent(
    data: com.sanidad.movil.data.remote.dto.DashboardResponseDTO,
    pendingAlerts: Int,
    pendingRecs: Int,
    lastUpdated: String?,
    onNavigateAlerts: () -> Unit,
    onNavigateRecommendations: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        DashboardHeader(lastUpdated, onRefresh, onLogout)
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                KpiGrid(
                    ventasHoy = data.ventasDelDia?.totalVentas ?: 0.0,
                    ventasMes = data.ventasMesActual ?: 0.0,
                    facturasHoy = (data.ventasDelDia?.cantidadVentas ?: 0).toInt(), // CORREGIDO: conversión a Int
                    pendingRecs = pendingRecs,
                    pendingAlerts = pendingAlerts,
                    onAlertsClick = onNavigateAlerts,
                    onRecsClick = onNavigateRecommendations
                )
            }
            item { ChartsGrid(data) }
            // BottomTables se puede descomentar cuando UserSession esté disponible y el DTO esté completo
            // item { BottomTables(data) }
        }
    }
}

@Composable
private fun DashboardHeader(lastUpdated: String?, onRefresh: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
            if (lastUpdated != null) {
                Text("Sincronizado: $lastUpdated", fontSize = 12.sp, color = Slate400)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null, tint = Success)
            }
            TextButton(onClick = onLogout) {
                Text("Salir", color = Slate600)
            }
        }
    }
}

@Composable
private fun KpiGrid(
    ventasHoy: Double,
    ventasMes: Double,
    facturasHoy: Int,
    pendingRecs: Int,
    pendingAlerts: Int,
    onAlertsClick: () -> Unit,
    onRecsClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Ventas Hoy", formatCurrency(ventasHoy), Modifier.weight(1f))
        KpiCard("Ventas Mes", formatCurrency(ventasMes), Modifier.weight(1f))
        KpiCard("Facturas Hoy", "$facturasHoy", Modifier.weight(1f))

        // Recomendaciones
        Card(
            modifier = Modifier.weight(1f).clickable { onRecsClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoLight)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Surface(modifier = Modifier.align(Alignment.TopEnd), shape = RoundedCornerShape(20.dp), color = IndigoLight) {
                    Text("Sugerencia", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IndigoText)
                }
                Column {
                    Icon(Icons.Default.Lightbulb, null, tint = IndigoText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Recomendaciones", fontSize = 11.sp, color = Slate500)
                    Text("$pendingRecs", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IndigoText)
                    Text("Optimización de stock", fontSize = 10.sp, color = Slate400)
                }
            }
        }
        // Alertas
        Card(
            modifier = Modifier.weight(1f).clickable { onAlertsClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Surface(modifier = Modifier.align(Alignment.TopEnd), shape = RoundedCornerShape(20.dp), color = Danger.copy(alpha = 0.1f)) {
                    Text("Crítico", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Danger)
                }
                Column {
                    Icon(Icons.Default.Notifications, null, tint = Danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Alertas Pendientes", fontSize = 11.sp, color = Slate500)
                    Text("$pendingAlerts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Danger)
                    Text("Revisar centro de control", fontSize = 10.sp, color = Slate400)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title.uppercase(), fontSize = 11.sp, color = Slate500)
            Spacer(Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
        }
    }
}

@Composable
private fun ChartsGrid(data: com.sanidad.movil.data.remote.dto.DashboardResponseDTO) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartCard("Ventas por Vendedor", Modifier.weight(1f)) {
                val entries = data.rankingVendedores?.map { it.totalVentas.toFloat() } ?: emptyList()
                if (entries.isNotEmpty()) {
                    Chart(
                        chart = columnChart(),
                        model = entryModelOf(*entries.toTypedArray()),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis()
                    )
                } else {
                    Text("Sin datos", color = Slate400)
                }
            }
            ChartCard("Tendencia de Ventas", Modifier.weight(1f)) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartCard("Productos Más Rentables", Modifier.weight(1f)) {
                val pieData = data.productosMasRentables?.map { it.nombre to it.ingresos.toFloat() } ?: emptyList()
                if (pieData.isNotEmpty()) SimplePieChart(pieData)
                else Text("Sin datos", color = Slate400)
            }
            ChartCard("Clientes Frecuentes", Modifier.weight(1f)) {
                Text("Próximamente", color = Slate400)
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.height(260.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Slate700, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun SimplePieChart(data: List<Pair<String, Float>>) {
    val colors = listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFF59E0B), Color(0xFFEF4444))
    val total = data.sumOf { it.second.toDouble() }.toFloat()
    if (total == 0f) return
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(120.dp).weight(1f)) {
            var start = -90f
            data.forEachIndexed { i, p ->
                val sweep = (p.second / total) * 360f
                drawArc(color = colors[i % colors.size], startAngle = start, sweepAngle = sweep, useCenter = true, size = Size(size.width, size.height))
                start += sweep
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            data.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(colors[i % colors.size]) }
                    Spacer(Modifier.width(4.dp))
                    Text(p.first, fontSize = 10.sp, color = Slate600, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(formatCurrency(p.second.toDouble()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate800)
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)
}