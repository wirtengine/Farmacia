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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Paleta unificada con el resto de la app
private val Primary = Color(0xFF4F46E5)
private val PrimaryGreen = Color(0xFF10B981)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val wideMode = configuration.screenWidthDp > 600

    Scaffold(containerColor = Slate50) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header unificado
            AlertsHeader(
                lastUpdated = state.lastUpdated,
                onSync = { viewModel.generarAlertas() },
                isLoading = state.isLoading
            )

            Spacer(Modifier.height(20.dp))

            // KPIs en fila adaptativa (reemplaza LazyVerticalGrid para evitar conflictos de altura infinita)
            AlertsKpiRow(
                total = state.total,
                pending = state.pending,
                high = state.high,
                resolved = state.resolved
            )

            // Mensaje de error
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    ErrorToast(message = error) { viewModel.clearError() }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Filtro de estado como chips (experimental, cubierto por @OptIn)
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filtros = mapOf(
                    "ALL" to "Todas",
                    "PENDING" to "Pendientes",
                    "RESOLVED" to "Resueltas"
                )
                filtros.forEach { (key, label) ->
                    FilterChip(
                        selected = state.statusFilter == key,
                        onClick = { viewModel.setStatusFilter(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Contenido principal: tabla o tarjetas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                when {
                    state.isLoading && state.alerts.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }
                    state.filteredAlerts.isEmpty() -> {
                        EmptyState(state.statusFilter)
                    }
                    wideMode -> {
                        // Modo tabla (pantalla ancha)
                        AlertsTable(
                            alerts = state.filteredAlerts,
                            onAcknowledge = { viewModel.atenderAlerta(it) }
                        )
                    }
                    else -> {
                        // Modo tarjetas (vertical)
                        AlertsCardList(
                            alerts = state.filteredAlerts,
                            onAcknowledge = { viewModel.atenderAlerta(it) }
                        )
                    }
                }
            }
        }
    }
}

// ---------- HEADER ----------
@Composable
private fun AlertsHeader(
    lastUpdated: String?,
    onSync: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Centro de Control", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
            if (lastUpdated != null) {
                Text("Última actualización: $lastUpdated", fontSize = 14.sp, color = Slate500)
            }
        }
        Button(
            onClick = onSync,
            enabled = !isLoading,
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sincronizar", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

// ---------- KPI ROW (sin scroll, siempre visible) ----------
@Composable
private fun AlertsKpiRow(total: Int, pending: Int, high: Int, resolved: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiPanel(
            title = "Total Alertas",
            value = total.toString(),
            icon = Icons.Default.Notifications,
            bgColor = Color(0xFFF0FDF4),
            iconColor = PrimaryGreen,
            modifier = Modifier.weight(1f)
        )
        KpiPanel(
            title = "Pendientes",
            value = pending.toString(),
            icon = Icons.Default.Schedule,
            bgColor = Color(0xFFFFFBEB),
            iconColor = if (pending > 0) Warning else Slate400,
            modifier = Modifier.weight(1f)
        )
        KpiPanel(
            title = "Críticas / Altas",
            value = high.toString(),
            icon = Icons.Default.Shield,
            bgColor = Color(0xFFFEF2F2),
            iconColor = Danger,
            subtitle = "Resueltas: $resolved",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiPanel(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 11.sp, color = Slate400)
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ---------- ERROR TOAST ----------
@Composable
private fun ErrorToast(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = Color(0xFF991B1B), fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, null, tint = Danger)
            }
        }
    }
}

// ---------- EMPTY STATE ----------
@Composable
private fun EmptyState(filter: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = Success)
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    "PENDING" -> "No hay alertas pendientes."
                    "RESOLVED" -> "No hay alertas resueltas."
                    else -> "No hay alertas registradas."
                },
                color = Slate400,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------- MODO TABLA (PANTALLA ANCHA) ----------
@Composable
private fun AlertsTable(
    alerts: List<com.sanidad.movil.data.remote.dto.AlertResponse>,
    onAcknowledge: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Encabezados de columna
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TableHeader("Alerta / Descripción", Modifier.weight(2.5f))
                TableHeader("Severidad", Modifier.weight(1f))
                TableHeader("Estado", Modifier.weight(1f))
                TableHeader("Fecha", Modifier.weight(1f))
                TableHeader("Acción", Modifier.weight(1f), alignEnd = true)
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(alert = alert, onAcknowledge = onAcknowledge)
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    alert: com.sanidad.movil.data.remote.dto.AlertResponse,
    onAcknowledge: (Long) -> Unit
) {
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
    val statusColor = if (alert.status == "PENDING") Slate700 else Color(0xFF15803D)
    val statusBg = if (alert.status == "PENDING") Slate100 else Color(0xFFDCFCE7)
    val statusText = when (alert.status) {
        "PENDING" -> "Pendiente"
        "RESOLVED", "ACKNOWLEDGED" -> "Atendida"
        else -> alert.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(2.5f)) {
                Text(alert.title, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                Text(alert.description, color = Slate500, fontSize = 13.sp)
            }
            Surface(shape = RoundedCornerShape(40.dp), color = severityBg) {
                Text(alert.severity, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = severityColor)
            }
            Surface(shape = RoundedCornerShape(40.dp), color = statusBg) {
                Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
            Text(alert.createdAt?.take(10) ?: "-", fontSize = 13.sp, color = Slate500)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (alert.status == "PENDING") {
                    TextButton(
                        onClick = { onAcknowledge(alert.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                    ) {
                        Text("Atender", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---------- MODO TARJETAS (VERTICAL) ----------
@Composable
private fun AlertsCardList(
    alerts: List<com.sanidad.movil.data.remote.dto.AlertResponse>,
    onAcknowledge: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert, onAcknowledge = onAcknowledge)
        }
    }
}

@Composable
private fun AlertCard(
    alert: com.sanidad.movil.data.remote.dto.AlertResponse,
    onAcknowledge: (Long) -> Unit
) {
    val severityColor = when (alert.severity) {
        "ALTA", "CRITICAL" -> Danger
        "MEDIA" -> Warning
        else -> Primary
    }
    val statusColor = if (alert.status == "PENDING") Slate700 else Success
    val statusText = when (alert.status) {
        "PENDING" -> "Pendiente"
        "RESOLVED", "ACKNOWLEDGED" -> "Atendida"
        else -> alert.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.title, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(alert.description, color = Slate500, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = severityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        alert.severity,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(alert.createdAt?.take(10) ?: "-", fontSize = 13.sp, color = Slate400)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp), color = if (alert.status == "PENDING") Slate100 else Success.copy(alpha = 0.1f)) {
                        Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                    Spacer(Modifier.width(12.dp))
                    if (alert.status == "PENDING") {
                        TextButton(
                            onClick = { onAcknowledge(alert.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Text("Atender", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(20.dp))
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate500,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}