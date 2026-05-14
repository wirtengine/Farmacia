package com.sanidad.movil.data.presentation.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
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

    val dashboardData by viewModel.dashboardData.collectAsState()
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()
    val pendingRecs by viewModel.pendingRecs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Dashboard")
                },
                actions = {

                    Text(
                        text = lastUpdated ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )

                    IconButton(
                        onClick = { viewModel.cargarDatos() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }

                    TextButton(
                        onClick = onLogout
                    ) {
                        Text("Salir")
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else {

            dashboardData?.let { data ->

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ---------------------------------------------------
                    // KPIs
                    // ---------------------------------------------------

                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            KpiCard(
                                title = "Ventas Hoy",
                                value = formatCurrency(data.ventasDelDia.totalVentas),
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = "Ventas Mes",
                                value = formatCurrency(data.ventasMesActual),
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = "Facturas Hoy",
                                value = "${data.ventasDelDia.cantidadVentas}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ---------------------------------------------------
                    // Alertas y recomendaciones
                    // ---------------------------------------------------

                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onNavigateRecommendations()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE8EAF6)
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF4338CA)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Recomendaciones",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "$pendingRecs",
                                        fontSize = 24.sp,
                                        color = Color(0xFF4338CA)
                                    )

                                    Text(
                                        text = "Optimización de stock",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onNavigateAlerts()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6F00)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Alertas",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "$pendingAlerts",
                                        fontSize = 24.sp,
                                        color = Color(0xFFFF6F00)
                                    )

                                    Text(
                                        text = "Pendientes",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------
                    // Gráfico de barras
                    // ---------------------------------------------------

                    item {

                        ChartCard(
                            title = "Ventas por Vendedor"
                        ) {

                            ColumnChartComponent(
                                entries = data.rankingVendedores.map {
                                    it.totalVentas.toFloat()
                                }
                            )
                        }
                    }

                    // ---------------------------------------------------
                    // Pie chart
                    // ---------------------------------------------------

                    item {

                        ChartCard(
                            title = "Productos Más Rentables"
                        ) {

                            SimplePieChart(
                                data = data.productosMasRentables.map {
                                    it.nombre to it.ingresos.toFloat()
                                }
                            )
                        }
                    }

                    // ---------------------------------------------------
                    // Línea
                    // ---------------------------------------------------

                    item {

                        ChartCard(
                            title = "Tendencia de Ventas"
                        ) {

                            LineChartComponent(
                                entries = listOf(
                                    data.ventasMesAnterior.toFloat(),
                                    data.ventasMesActual.toFloat()
                                )
                            )
                        }
                    }

                    // ---------------------------------------------------
                    // Ranking
                    // ---------------------------------------------------

                    item {

                        Text(
                            text = "Ranking de Vendedores",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        data.rankingVendedores.forEachIndexed { index, vendedor ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text("${index + 1}. ${vendedor.username}")

                                Text("${vendedor.cantidadVentas} ventas")

                                Text(
                                    formatCurrency(vendedor.totalVentas)
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------
                    // Stock crítico
                    // ---------------------------------------------------

                    item {

                        Text(
                            text = "Stock Crítico",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        data.productosBajoStock.forEach { producto ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(producto.nombre)

                                Text(
                                    text = "${producto.stockTotal} uds",
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }

            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {

                Text("No se pudo cargar el dashboard")
            }
        }
    }
}

// =====================================================
// KPI CARD
// =====================================================

@Composable
fun KpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =====================================================
// CHART CARD
// =====================================================

@Composable
fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                content()
            }
        }
    }
}

// =====================================================
// COLUMN CHART
// =====================================================

@Composable
fun ColumnChartComponent(
    entries: List<Float>
) {

    Chart(
        chart = columnChart(),
        model = entryModelOf(*entries.toTypedArray()),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis()
    )
}

// =====================================================
// LINE CHART
// =====================================================

@Composable
fun LineChartComponent(
    entries: List<Float>
) {

    Chart(
        chart = lineChart(),
        model = entryModelOf(*entries.toTypedArray()),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis()
    )
}

// =====================================================
// PIE CHART
// =====================================================

@Composable
fun SimplePieChart(
    data: List<Pair<String, Float>>
) {

    val colors = listOf(
        Color(0xFF10B981),
        Color(0xFF3B82F6),
        Color(0xFF6366F1),
        Color(0xFFF59E0B),
        Color(0xFFEF4444)
    )

    val total = data.sumOf { it.second.toDouble() }.toFloat()

    if (total == 0f) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Canvas(
            modifier = Modifier.size(150.dp)
        ) {

            var startAngle = -90f

            data.forEachIndexed { index, pair ->

                val sweepAngle = (pair.second / total) * 360f

                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )

                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height),
                    style = Stroke(width = 4f)
                )

                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {

            data.forEachIndexed { index, pair ->

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {

                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        drawCircle(
                            color = colors[index % colors.size]
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = pair.first,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatCurrency(pair.second.toDouble()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =====================================================
// FORMATO MONEDA
// =====================================================

fun formatCurrency(
    value: Double
): String {

    val format = NumberFormat.getCurrencyInstance(
        Locale("es", "NI")
    )

    return format.format(value)
}