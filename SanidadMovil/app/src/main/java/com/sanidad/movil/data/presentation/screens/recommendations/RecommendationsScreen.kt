package com.sanidad.movil.data.presentation.screens.recommendations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
fun RecommendationsScreen(viewModel: RecommendationsViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarRecomendaciones() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motor de Recomendaciones") },
                actions = {
                    Text(lastUpdated, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // KPIs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    title = "Total Analizado",
                    value = "${viewModel.total}",
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter("ALL") }
                )
                KpiCard(
                    title = "Pendientes",
                    value = "${viewModel.pending}",
                    color = if (viewModel.pending > 0) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter("PENDING") }
                )
                KpiCard(
                    title = "Aplicadas",
                    value = "${viewModel.accepted}",
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter("ACCEPTED") }
                )
                KpiCard(
                    title = "Descartadas",
                    value = "${viewModel.dismissed}",
                    color = Color(0xFFFCE4EC),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setStatusFilter("DISMISSED") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón generar
            Button(
                onClick = { viewModel.generarRecomendaciones() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Analizando..." else "Actualizar Análisis")
            }

            // Error
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtro
            Text("Sugerencias Optimizadas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "PENDING" to "Pendientes",
                    "ACCEPTED" to "Aceptadas",
                    "DISMISSED" to "Descartadas",
                    "ALL" to "Todas"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { viewModel.setStatusFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && viewModel.total == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val filtered = viewModel.filteredRecs
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay recomendaciones en esta categoría.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered) { rec ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(viewModel.getTypeLabel(rec.type), fontWeight = FontWeight.Bold)
                                        Text(
                                            viewModel.getPriorityLabel(rec.priority),
                                            color = when (rec.priority.uppercase()) {
                                                "HIGH" -> Color(0xFFD32F2F)
                                                "MEDIUM" -> Color(0xFFF57C00)
                                                else -> Color(0xFF388E3C)
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rec.title, fontWeight = FontWeight.Bold)
                                    Text(rec.description, fontSize = 12.sp)
                                    rec.suggestedAction?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, fontSize = 12.sp, color = Color(0xFF1565C0))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Creada: ${rec.createdAt}", fontSize = 11.sp)
                                        Text(
                                            viewModel.getStatusDisplay(rec.status),
                                            color = when (rec.status.uppercase()) {
                                                "PENDING" -> Color(0xFFFFA000)
                                                "ACCEPTED", "RESOLVED" -> Color(0xFF388E3C)
                                                "DISMISSED" -> Color(0xFF757575)
                                                else -> Color.Gray
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (rec.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { viewModel.aceptarRecomendacion(rec.id) }) {
                                                Text("Aceptar", color = Color(0xFF4CAF50))
                                            }
                                            TextButton(onClick = { viewModel.descartarRecomendacion(rec.id) }) {
                                                Text("Descartar", color = Color(0xFFF44336))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}