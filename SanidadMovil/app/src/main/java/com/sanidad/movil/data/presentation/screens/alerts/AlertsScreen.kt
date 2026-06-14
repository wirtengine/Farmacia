package com.sanidad.movil.presentation.screens.alerts

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Paleta exacta del CSS original
private val PrimaryGreen = Color(0xFF10B981)
private val PrimaryDark = Color(0xFF065F46)
private val DangerColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)
private val InfoBg = Color(0xFFF0FDF4)
private val WarningBg = Color(0xFFFFFBEB)
private val CriticalBg = Color(0xFFFEF2F2)
private val TextMain = Color(0xFF111827)
private val TextMuted = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)
private val BgLight = Color(0xFFF9FAFB)

@Composable
fun AlertsScreen(viewModel: AlertsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // ---- HEADER ----
            AlertsHeader(
                lastUpdated = state.lastUpdated,
                onSync = { viewModel.generarAlertas() },
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(30.dp))

            // ---- KPI GRID ----
            AlertsKpiGrid(
                total = state.total,
                pending = state.pending,
                high = state.high,
                resolved = state.resolved
            )

            Spacer(modifier = Modifier.height(30.dp))

            // ---- MENSAJE DE ERROR ----
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    ErrorToast(message = error) { viewModel.clearError() }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- ÁREA DE CONTENIDO (TABLA) ----
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    // Título y filtro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Listado de Alertas del Sistema",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        FilterDropdown(
                            currentFilter = state.statusFilter,
                            onFilterSelected = { viewModel.setStatusFilter(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isLoading && state.alerts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    } else if (state.filteredAlerts.isEmpty()) {
                        EmptyState(state.statusFilter)
                    } else {
                        // Tabla de alertas
                        AlertsTable(
                            alerts = state.filteredAlerts,
                            onAcknowledge = { viewModel.atenderAlerta(it) }
                        )
                    }
                }
            }
        }
    }
}

// ---------- COMPONENTES DE LA UI ----------

@Composable
private fun AlertsHeader(
    lastUpdated: String?,
    onSync: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Centro de Control",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
            if (lastUpdated != null) {
                Text(
                    text = "Última actualización: $lastUpdated",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
        }
        Button(
            onClick = onSync,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Sincronizar Alertas", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlertsKpiGrid(total: Int, pending: Int, high: Int, resolved: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        KpiPanel(
            title = "Total Alertas",
            value = total.toString(),
            icon = Icons.Default.Notifications,
            backgroundColor = InfoBg,
            iconColor = PrimaryGreen,
            modifier = Modifier.weight(1f)
        )
        KpiPanel(
            title = "Pendientes",
            value = pending.toString(),
            icon = Icons.Default.Schedule,
            backgroundColor = WarningBg,
            iconColor = if (pending > 0) WarningColor else TextMuted,
            valueColor = if (pending > 0) WarningColor else TextMain,
            modifier = Modifier.weight(1f)
        )
        KpiPanel(
            title = "Críticas / Altas",
            value = high.toString(),
            subtitle = "Resueltas: $resolved",
            icon = Icons.Default.Shield,
            backgroundColor = CriticalBg,
            iconColor = DangerColor,
            valueColor = DangerColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiPanel(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    valueColor: Color = TextMain,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ErrorToast(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, DangerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = DangerColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = Color(0xFF991B1B), fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = DangerColor)
            }
        }
    }
}

@Composable
private fun FilterDropdown(currentFilter: String, onFilterSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = mapOf(
        "PENDING" to "Pendientes",
        "RESOLVED" to "Atendidas / Resueltas",
        "ALL" to "Todas"
    )
    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .clickable { expanded = true },
            color = BgLight
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(options[currentFilter] ?: "Pendientes", color = TextMain)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onFilterSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(filter: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = PrimaryGreen
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (filter) {
                "PENDING" -> "No hay alertas pendientes en este momento."
                "RESOLVED" -> "No hay alertas atendidas o resueltas."
                else -> "No hay alertas activas en este momento."
            },
            color = TextMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlertsTable(
    alerts: List<com.sanidad.movil.data.remote.dto.AlertResponse>,
    onAcknowledge: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // Encabezado de la tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(2.dp, BgLight, RoundedCornerShape(0.dp)), // solo borde inferior grueso
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TableHeader("Alerta / Descripción", Modifier.weight(2f))
                TableHeader("Severidad", Modifier.weight(1f))
                TableHeader("Estado", Modifier.weight(1f))
                TableHeader("Fecha", Modifier.weight(1f))
                TableHeader("Acción", Modifier.weight(1f), alignEnd = true)
            }
            Spacer(Modifier.height(8.dp))
        }

        items(alerts, key = { it.id }) { alert ->
            val severityColor = when (alert.severity) {
                "ALTA", "CRITICAL" -> Color(0xFFB91C1C)
                "MEDIA" -> Color(0xFFB45309)
                else -> Color(0xFF0369A1)
            }
            val severityBg = when (alert.severity) {
                "ALTA", "CRITICAL" -> Color(0xFFFEE2E2)
                "MEDIA" -> Color(0xFFFEF3C7)
                else -> Color(0xFFE0F2FE)
            }
            val statusColor = when (alert.status) {
                "PENDING" -> Color(0xFF475569)
                "ACKNOWLEDGED", "RESOLVED" -> Color(0xFF15803D)
                else -> TextMuted
            }
            val statusBg = when (alert.status) {
                "PENDING" -> Color(0xFFF1F5F9)
                else -> Color(0xFFDCFCE7)
            }
            val statusText = when (alert.status) {
                "PENDING" -> "Pendiente"
                "ACKNOWLEDGED" -> "Atendida"
                "RESOLVED" -> "Resuelta"
                else -> alert.status
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(alert.title, fontWeight = FontWeight.Bold, color = TextMain, fontSize = 15.sp)
                        Text(alert.description, color = TextMuted, fontSize = 13.sp)
                    }
                    // Severidad
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(40.dp))
                            .background(severityBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            alert.severity,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }
                    // Estado
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(40.dp))
                            .background(statusBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                    // Fecha
                    Text(
                        text = alert.createdAt?.take(10) ?: "-",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    // Acción
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (alert.status == "PENDING") {
                            TextButton(
                                onClick = { onAcknowledge(alert.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                            ) {
                                Text("Atender", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        ),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}