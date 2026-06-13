package com.sanidad.movil.data.presentation.screens.perdidas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerdidasScreen(viewModel: PerdidasViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resumen by viewModel.resumen.collectAsState()

    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { viewModel.cargarDatos() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Control de Pérdidas") }) }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.cargarDatos() }) { Text("Reintentar") }
                }
            }
            else -> {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    // KPIs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiCard(
                            title = "Pérdidas por Venc.",
                            value = viewModel.formatCurrency(resumen?.totalPerdidasVencimiento ?: 0.0),
                            subtitle = "${resumen?.cantidadProductosVencidos ?: 0} productos",
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Inmovilizado",
                            value = viewModel.formatCurrency(resumen?.totalInmovilizado ?: 0.0),
                            subtitle = "${resumen?.cantidadProductosInmoviles ?: 0} SKU",
                            color = Color(0xFFE3F2FD),
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Inconsistencias",
                            value = "${resumen?.cantidadInconsistencias ?: 0}",
                            subtitle = "detectadas",
                            color = Color(0xFFFCE4EC),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pestañas
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Vencidos") })
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Sin Rotación") })
                        Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Inconsistencias") })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (activeTab) {
                        0 -> VencidosTab(viewModel)
                        1 -> InmovilesTab(viewModel)
                        2 -> InconsistenciasTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 11.sp)
        }
    }
}

@Composable
fun VencidosTab(viewModel: PerdidasViewModel) {
    val vencidos by viewModel.vencidos.collectAsState()
    if (vencidos.isEmpty()) {
        EmptyState(message = "No se registran productos vencidos con stock.")
    } else {
        LazyColumn {
            items(vencidos) { v ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Lote: ${v.numeroLote}", fontWeight = FontWeight.Bold)
                        Text(v.medicamentoNombre)
                        Text("Vence: ${v.fechaVencimiento}")
                        Text("Cantidad: ${v.cantidadVencida} u.")
                        Text("Valor perdido: ${viewModel.formatCurrency(v.valorPerdido)}", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun InmovilesTab(viewModel: PerdidasViewModel) {
    val inmoviles by viewModel.inmoviles.collectAsState()
    if (inmoviles.isEmpty()) {
        EmptyState(message = "Todos los productos presentan rotación activa.")
    } else {
        LazyColumn {
            items(inmoviles) { p ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(p.medicamentoNombre, fontWeight = FontWeight.Bold)
                        Text("Stock: ${p.stockActual} u.")
                        Text("Días sin movimiento: ${p.diasSinMovimiento}")
                        Text("Valor inmovilizado: ${viewModel.formatCurrency(p.valorInmovilizado)}")
                    }
                }
            }
        }
    }
}

@Composable
fun InconsistenciasTab(viewModel: PerdidasViewModel) {
    val inconsistencias by viewModel.inconsistencias.collectAsState()
    if (inconsistencias.isEmpty()) {
        EmptyState(message = "Integridad de stock verificada.")
    } else {
        LazyColumn {
            items(inconsistencias) { inc ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(inc.medicamentoNombre, fontWeight = FontWeight.Bold)
                        Text("Stock lote: ${inc.cantidadLote}")
                        Text("Stock ubicación: ${inc.cantidadUbicaciones}")
                        Text(
                            "Diferencia: ${if (inc.diferencia > 0) "+${inc.diferencia}" else "${inc.diferencia}"}",
                            color = if (inc.diferencia > 0) Color(0xFFF57C00) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}