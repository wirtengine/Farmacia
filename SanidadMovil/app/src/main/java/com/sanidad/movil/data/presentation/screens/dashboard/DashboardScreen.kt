package com.sanidad.movil.presentation.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.pullRefresh
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateAlerts: () -> Unit = {},
    onNavigateRecommendations: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    Text(uiState.lastUpdated ?: "", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                    TextButton(onClick = onLogout) {
                        Text("Salir")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    EmptyErrorState(uiState.error!!, onRetry = { viewModel.loadData() })
                }
                uiState.dashboard == null -> {
                    EmptyErrorState("No hay datos disponibles", onRetry = { viewModel.loadData() })
                }
                else -> {
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadData() }
                    )
                    Box(Modifier.pullRefresh(pullRefreshState)) {
                        DashboardContent(
                            data = uiState.dashboard!!,
                            pendingAlerts = uiState.pendingAlerts,
                            pendingRecs = uiState.pendingRecs,
                            onNavigateAlerts = onNavigateAlerts,
                            onNavigateRecommendations = onNavigateRecommendations
                        )
                        PullRefreshIndicator(
                            refreshing = uiState.isLoading,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
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
        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun DashboardContent(
    data: com.sanidad.movil.data.remote.dto.DashboardResponseDTO,
    pendingAlerts: Int,
    pendingRecs: Int,
    onNavigateAlerts: () -> Unit,
    onNavigateRecommendations: () -> Unit
) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Ventas Hoy", formatCurrency(data.ventasDelDia.totalVentas), Modifier.weight(1f))
                KpiCard("Ventas Mes", formatCurrency(data.ventasMesActual), Modifier.weight(1f))
                KpiCard("Facturas Hoy", "${data.ventasDelDia.cantidadVentas}", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    Modifier.weight(1f).clickable { onNavigateRecommendations() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFF4338CA))
                        Spacer(Modifier.height(8.dp))
                        Text("Recomendaciones", fontWeight = FontWeight.Bold)
                        Text("$pendingRecs", fontSize = 24.sp, color = Color(0xFF4338CA))
                        Text("Optimización de stock", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(
                    Modifier.weight(1f).clickable { onNavigateAlerts() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFFFF6F00))
                        Spacer(Modifier.height(8.dp))
                        Text("Alertas", fontWeight = FontWeight.Bold)
                        Text("$pendingAlerts", fontSize = 24.sp, color = Color(0xFFFF6F00))
                        Text("Pendientes", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            ChartCard("Ventas por Vendedor") {
                ColumnChartComponent(data.rankingVendedores.map { it.totalVentas.toFloat() })
            }
        }
        item {
            ChartCard("Productos Más Rentables") {
                SimplePieChart(data.productosMasRentables.map { it.nombre to it.ingresos.toFloat() })
            }
        }
        item {
            ChartCard("Tendencia de Ventas") {
                LineChartComponent(listOf(data.ventasMesAnterior.toFloat(), data.ventasMesActual.toFloat()))
            }
        }
        item {
            Text("Ranking de Vendedores", style = MaterialTheme.typography.titleLarge)
            data.rankingVendedores.forEachIndexed { i, v ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${i + 1}. ${v.username}")
                    Text("${v.cantidadVentas} ventas")
                    Text(formatCurrency(v.totalVentas))
                }
            }
        }
        item {
            Text("Stock Crítico", style = MaterialTheme.typography.titleLarge)
            data.productosBajoStock.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.nombre)
                    Text("${p.stockTotal} uds", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(220.dp)) { content() }
        }
    }
}

@Composable
fun ColumnChartComponent(entries: List<Float>) {
    Chart(
        chart = columnChart(),
        model = entryModelOf(*entries.toTypedArray()),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis()
    )
}

@Composable
fun LineChartComponent(entries: List<Float>) {
    Chart(
        chart = lineChart(),
        model = entryModelOf(*entries.toTypedArray()),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis()
    )
}

@Composable
fun SimplePieChart(data: List<Pair<String, Float>>) {
    val colors = listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFF59E0B), Color(0xFFEF4444))
    val total = data.sumOf { it.second.toDouble() }.toFloat()
    if (total == 0f) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(150.dp)) {
            var start = -90f
            data.forEachIndexed { i, p ->
                val sweep = (p.second / total) * 360f
                drawArc(colors[i % colors.size], start, sweep, true, Size(size.width, size.height))
                start += sweep
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            data.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Canvas(Modifier.size(12.dp)) { drawCircle(colors[i % colors.size]) }
                    Spacer(Modifier.width(6.dp))
                    Text(p.first, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(formatCurrency(p.second.toDouble()), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)