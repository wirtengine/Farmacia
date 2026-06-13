package com.sanidad.movil.data.presentation.screens.alerts

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
fun AlertsScreen(viewModel: AlertsViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarAlertas() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Control") },
                actions = {
                    Text(
                        lastUpdated,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                    title = "Total Alertas",
                    value = "${viewModel.total}",
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Pendientes",
                    value = "${viewModel.pending}",
                    color = if (viewModel.pending > 0) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Críticas/Altas",
                    value = "${viewModel.high}",
                    subtitle = "Resueltas: ${viewModel.resolved}",
                    color = Color(0xFFFCE4EC),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón sincronizar
            Button(
                onClick = { viewModel.generarAlertas() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Sincronizar Alertas")
            }

            // Error
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtro
            Text("Listado de Alertas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "PENDING" to "Pendientes",
                    "RESOLVED" to "Atendidas/Resueltas",
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

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val filtered = viewModel.filteredAlerts
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay alertas en esta categoría.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered) { alert ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(alert.title, fontWeight = FontWeight.Bold)
                                    Text(alert.description, fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            alert.severity,
                                            color = when (alert.severity) {
                                                "ALTA", "CRITICAL" -> Color(0xFFD32F2F)
                                                "MEDIA" -> Color(0xFFF57C00)
                                                else -> Color(0xFF388E3C)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            viewModel.getStatusText(alert.status),
                                            color = when (alert.status) {
                                                "PENDING" -> Color(0xFFFFA000)
                                                "ACKNOWLEDGED" -> Color(0xFF1976D2)
                                                "RESOLVED" -> Color(0xFF388E3C)
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                    Text("Creada: ${alert.createdAt}", fontSize = 11.sp)
                                    if (alert.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.atenderAlerta(alert.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) { Text("Atender") }
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
fun KpiCard(title: String, value: String, subtitle: String? = null, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, fontSize = 10.sp)
            }
        }
    }
}