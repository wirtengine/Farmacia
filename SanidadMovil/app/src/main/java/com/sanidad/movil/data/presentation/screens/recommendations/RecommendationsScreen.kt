package com.sanidad.movil.presentation.screens.recommendations

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import java.text.SimpleDateFormat
import java.util.*

// Paleta fiel al CSS
private val TextMain = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val BgLight = Color(0xFFF8FAFC)
private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Warning = Color(0xFFF59E0B)
private val Danger = Color(0xFFEF4444)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)

@Composable
fun RecommendationsScreen(viewModel: RecommendationsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = BgLight) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // ---- HEADER ----
            RecommendationsHeader(
                lastUpdated = state.lastUpdated,
                onGenerate = { viewModel.generarRecomendaciones() },
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(30.dp))

            // ---- KPI GRID ----
            RecommendationsKpiGrid(
                total = state.total,
                pending = state.pending,
                accepted = state.accepted,
                dismissed = state.dismissed,
                onFilterSelected = { viewModel.setStatusFilter(it) }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // ---- MENSAJE DE ERROR ----
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.limpiarError() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = Danger)
                            }
                        }
                    }
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
                Column(modifier = Modifier.padding(24.dp)) {
                    // Título y filtro desplegable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sugerencias Optimizadas",
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

                    if (state.isLoading && state.recommendations.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.filteredRecs.isEmpty()) {
                        EmptyState(state.statusFilter)
                    } else {
                        RecommendationsTable(
                            recs = state.filteredRecs,
                            onAccept = { viewModel.aceptarRecomendacion(it) },
                            onDismiss = { viewModel.descartarRecomendacion(it) },
                            getPriorityLabel = { viewModel.getPriorityLabel(it) },
                            getStatusDisplay = { viewModel.getStatusDisplay(it) },
                            getTypeLabel = { viewModel.getTypeLabel(it) }
                        )
                    }
                }
            }
        }
    }
}

// ---------- COMPONENTES ----------

@Composable
private fun RecommendationsHeader(
    lastUpdated: String?,
    onGenerate: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                "Motor de Recomendaciones",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
            if (lastUpdated != null) {
                Text(
                    "Inteligencia de inventario • $lastUpdated",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
        }
        Button(
            onClick = onGenerate,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isLoading) "Analizando..." else "Actualizar Análisis", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecommendationsKpiGrid(
    total: Int,
    pending: Int,
    accepted: Int,
    dismissed: Int,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KpiCard(
            title = "Total Analizado",
            value = total.toString(),
            icon = Icons.Default.TrendingUp,
            backgroundColor = Color(0xFFE0F2FE),
            iconColor = Color(0xFF0284C7),
            onClick = { onFilterSelected("ALL") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Pendientes",
            value = pending.toString(),
            icon = Icons.Default.Refresh,
            backgroundColor = if (pending > 0) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
            iconColor = if (pending > 0) Warning else Slate500,
            onClick = { onFilterSelected("PENDING") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Aplicadas",
            value = accepted.toString(),
            icon = Icons.Default.CheckCircle,
            backgroundColor = Color(0xFFE8F5E9),
            iconColor = Success,
            onClick = { onFilterSelected("ACCEPTED") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Descartadas",
            value = dismissed.toString(),
            icon = Icons.Default.Cancel,
            backgroundColor = Color(0xFFFCE4EC),
            iconColor = Danger,
            onClick = { onFilterSelected("DISMISSED") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // CORRECCIÓN: título en mayúsculas sin textTransform
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun FilterDropdown(currentFilter: String, onFilterSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = mapOf(
        "PENDING" to "Pendientes",
        "ACCEPTED" to "Aplicadas / Resueltas",
        "DISMISSED" to "Descartadas",
        "ALL" to "Ver Todas"
    )
    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = BgLight
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(options[currentFilter] ?: "Pendientes", color = TextMain)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = TextMuted)
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
            null,
            modifier = Modifier.size(48.dp),
            tint = Success
        )
        Spacer(Modifier.height(16.dp))
        Text(
            when (filter) {
                "PENDING" -> "No hay recomendaciones pendientes."
                "ACCEPTED" -> "No hay recomendaciones aplicadas o resueltas."
                "DISMISSED" -> "No hay recomendaciones descartadas."
                else -> "No hay recomendaciones registradas."
            },
            color = TextMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecommendationsTable(
    recs: List<RecommendationResponse>,
    onAccept: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    getPriorityLabel: (String) -> String,
    getStatusDisplay: (String) -> String,
    getTypeLabel: (String) -> String
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // Encabezado de tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(2.dp, BgLight, RoundedCornerShape(0.dp)),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TableHeader("Tipo", Modifier.weight(1.5f))
                TableHeader("Prioridad", Modifier.weight(1f))
                TableHeader("Detalle", Modifier.weight(2.5f))
                TableHeader("Sugerencia", Modifier.weight(2f))
                TableHeader("Fecha", Modifier.weight(1.2f))
                TableHeader("Estado", Modifier.weight(1f))
                TableHeader("Acciones", Modifier.weight(1.5f), alignEnd = true)
            }
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(recs, key = { _, r -> r.id }) { _, rec ->
            val normStatus = RecommendationsUiState.normalizeStatus(rec.status)
            val displayStatus = getStatusDisplay(rec.status)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
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
                    // Tipo
                    Row(
                        modifier = Modifier.weight(1.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconBg = when (rec.type.uppercase()) {
                            "PURCHASE_SUGGESTION" -> Color(0xFFECFDF5)
                            "AVOID_RESTOCK" -> Color(0xFFFFF7ED)
                            "PRIORITIZE_SALE" -> Color(0xFFEFF6FF)
                            else -> Slate100
                        }
                        val iconTint = when (rec.type.uppercase()) {
                            "PURCHASE_SUGGESTION" -> Success
                            "AVOID_RESTOCK" -> Warning
                            "PRIORITIZE_SALE" -> Primary
                            else -> Slate500
                        }
                        val typeIcon = when (rec.type.uppercase()) {
                            "PURCHASE_SUGGESTION" -> Icons.Default.ShoppingCart
                            "AVOID_RESTOCK" -> Icons.Default.Warning
                            "PRIORITIZE_SALE" -> Icons.Default.TrendingUp
                            else -> Icons.Default.Star
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(typeIcon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(getTypeLabel(rec.type), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Prioridad
                    val priorityColor = when (rec.priority.uppercase()) {
                        "HIGH" -> Color(0xFFEF4444)
                        "MEDIUM" -> Color(0xFFD97706)
                        else -> Success
                    }
                    val priorityBg = when (rec.priority.uppercase()) {
                        "HIGH" -> Color(0xFFFEE2E2)
                        "MEDIUM" -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            getPriorityLabel(rec.priority),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Detalle
                    Column(modifier = Modifier.weight(2.5f)) {
                        Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextMain)
                        Text(rec.description, fontSize = 12.sp, color = TextMuted)
                    }

                    // Sugerencia
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(
                            rec.suggestedAction ?: "",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = Slate700
                        )
                    }

                    // Fecha
                    Text(
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                            try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(rec.createdAt) }
                            catch (e: Exception) { Date() }
                        ),
                        modifier = Modifier.weight(1.2f),
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    // Estado
                    val statusColor = when (normStatus) {
                        "PENDING" -> Slate500
                        "ACCEPTED" -> Color(0xFF15803D)
                        "DISMISSED" -> Color(0xFFB91C1C)
                        else -> Slate500
                    }
                    val statusBg = when (normStatus) {
                        "PENDING" -> Slate100
                        "ACCEPTED" -> Color(0xFFDCFCE7)
                        "DISMISSED" -> Color(0xFFFEE2E2)
                        else -> Slate100
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            displayStatus,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }

                    // Acciones
                    Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.CenterEnd) {
                        if (normStatus == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onAccept(rec.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Aceptar", tint = Success)
                                }
                                IconButton(
                                    onClick = { onDismiss(rec.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, "Descartar", tint = Danger)
                                }
                            }
                        } else {
                            Icon(
                                if (normStatus == "ACCEPTED") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                null,
                                tint = if (normStatus == "ACCEPTED") Success else Danger,
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
    // CORRECCIÓN: texto en mayúsculas sin textTransform
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}