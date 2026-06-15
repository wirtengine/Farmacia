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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import java.text.SimpleDateFormat
import java.util.*

// ── Paleta refinada (igual que Pérdidas) ─────────────────────────────────────
private val Indigo950  = Color(0xFF1E1B4B)
private val Indigo600  = Color(0xFF4F46E5)
private val Indigo100  = Color(0xFFE0E7FF)
private val Emerald600 = Color(0xFF059669)
private val Emerald50  = Color(0xFFECFDF5)
private val Rose600    = Color(0xFFDC2626)
private val Rose50     = Color(0xFFFFF1F2)
private val Amber600   = Color(0xFFD97706)
private val Amber50    = Color(0xFFFFFBEB)
private val Slate900   = Color(0xFF0F172A)
private val Slate600   = Color(0xFF475569)
private val Slate400   = Color(0xFF94A3B8)
private val Slate200   = Color(0xFFE2E8F0)
private val Slate100   = Color(0xFFF1F5F9)
private val Slate50    = Color(0xFFF8FAFC)
private val White      = Color.White

// ── Tabs ──────────────────────────────────────────────────────────────────────
private data class TabItem(val label: String, val icon: ImageVector, val key: String)
private val TABS = listOf(
    TabItem("Todas",       Icons.Default.List,       "ALL"),
    TabItem("Pendientes",  Icons.Default.Refresh,    "PENDING"),
    TabItem("Aplicadas",   Icons.Default.CheckCircle, "ACCEPTED"),
    TabItem("Descartadas", Icons.Default.Cancel,     "DISMISSED"),
)

// ── Pantalla principal ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(viewModel: RecommendationsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val wideMode = LocalConfiguration.current.screenWidthDp > 600

    Scaffold(containerColor = Slate50) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            RecommendationsHeader(
                lastUpdated = state.lastUpdated,
                onGenerate = { viewModel.generarRecomendaciones() },
                isLoading = state.isLoading
            )

            // KPI Row
            RecommendationsKpiRow(
                total = state.total,
                pending = state.pending,
                accepted = state.accepted,
                dismissed = state.dismissed,
                onFilterSelected = { viewModel.setStatusFilter(it) }
            )

            // Error banner
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { ErrorBanner(message = it) { viewModel.generarRecomendaciones() } }
            }

            Spacer(Modifier.height(20.dp))

            // Segmented tab control
            SegmentedTabBar(
                tabs       = TABS,
                activeKey  = state.statusFilter,
                onSelect   = { viewModel.setStatusFilter(it) },
                modifier   = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Contenido
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                when {
                    state.isLoading -> CenteredLoader()
                    else -> AnimatedContent(
                        targetState = state.statusFilter,
                        transitionSpec = {
                            fadeIn(androidx.compose.animation.core.tween(200)) togetherWith
                                    fadeOut(androidx.compose.animation.core.tween(150))
                        }
                    ) { filter ->
                        RecommendationsContent(
                            recs = state.filteredRecs,
                            onAccept = { viewModel.aceptarRecomendacion(it) },
                            onDismiss = { viewModel.descartarRecomendacion(it) },
                            getPriorityLabel = { viewModel.getPriorityLabel(it) },
                            getStatusDisplay = { viewModel.getStatusDisplay(it) },
                            getTypeLabel = { viewModel.getTypeLabel(it) },
                            wideMode = wideMode
                        )
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun RecommendationsHeader(
    lastUpdated: String?,
    onGenerate: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Motor de Recomendaciones",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Indigo950,
                letterSpacing = (-0.5).sp
            )
            if (lastUpdated != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Inteligencia de inventario • $lastUpdated",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald50)
                .clickable { onGenerate() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Emerald600,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Generar",
                    tint = Emerald600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    Divider(color = Slate200, thickness = 1.dp)
}

// ── KPI Row ───────────────────────────────────────────────────────────────────
@Composable
private fun RecommendationsKpiRow(
    total: Int,
    pending: Int,
    accepted: Int,
    dismissed: Int,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiCard(
            label = "Total",
            value = total.toString(),
            detail = "analizadas",
            accentColor = Slate600,
            bgColor = Slate100,
            onClick = { onFilterSelected("ALL") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label = "Pendientes",
            value = pending.toString(),
            detail = "por revisar",
            accentColor = if (pending > 0) Amber600 else Slate600,
            bgColor = if (pending > 0) Amber50 else Slate100,
            onClick = { onFilterSelected("PENDING") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label = "Aplicadas",
            value = accepted.toString(),
            detail = "ejecutadas",
            accentColor = Emerald600,
            bgColor = Emerald50,
            onClick = { onFilterSelected("ACCEPTED") },
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label = "Descartadas",
            value = dismissed.toString(),
            detail = "ignoradas",
            accentColor = Rose600,
            bgColor = Rose50,
            onClick = { onFilterSelected("DISMISSED") },
            modifier = Modifier.weight(1f)
        )
    }
    Divider(color = Slate200, thickness = 1.dp)
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    detail: String,
    accentColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Error Banner ──────────────────────────────────────────────────────────────
@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Rose50)
            .border(1.dp, Rose600.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null, tint = Rose600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = Rose600, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Rose600)
                .clickable { onRetry() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Reintentar", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Segmented Tab Bar ─────────────────────────────────────────────────────────
@Composable
private fun SegmentedTabBar(
    tabs: List<TabItem>,
    activeKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = tab.key == activeKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isActive) White else Color.Transparent)
                    .clickable { onSelect(tab.key) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (isActive) Indigo600 else Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) Indigo600 else Slate400,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Loader ────────────────────────────────────────────────────────────────────
@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Emerald600,
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ── Contenido de Recomendaciones (tabla / tarjetas) ──────────────────────────
@Composable
private fun RecommendationsContent(
    recs: List<RecommendationResponse>,
    onAccept: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    getPriorityLabel: (String) -> String,
    getStatusDisplay: (String) -> String,
    getTypeLabel: (String) -> String,
    wideMode: Boolean
) {
    if (recs.isEmpty()) {
        EmptyState()
        return
    }

    if (wideMode) {
        DataTable(
            headers = listOf(
                ColDef("Tipo", 1.5f),
                ColDef("Prioridad", 1f),
                ColDef("Detalle", 2.5f),
                ColDef("Sugerencia", 2f),
                ColDef("Fecha", 1.2f),
                ColDef("Estado", 1f),
                ColDef("Acciones", 1.5f, alignEnd = true)
            )
        ) {
            itemsIndexed(recs) { i, rec ->
                val normStatus = RecommendationsUiState.normalizeStatus(rec.status)
                DataRow(even = i % 2 == 0) {
                    // Tipo
                    Row(Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                        val typeColor = when (rec.type.uppercase()) {
                            "PURCHASE_SUGGESTION" -> Emerald600
                            "AVOID_RESTOCK" -> Amber600
                            "PRIORITIZE_SALE" -> Indigo600
                            else -> Slate600
                        }
                        val typeBg = typeColor.copy(alpha = 0.1f)
                        val typeIcon = when (rec.type.uppercase()) {
                            "PURCHASE_SUGGESTION" -> Icons.Default.ShoppingCart
                            "AVOID_RESTOCK" -> Icons.Default.Warning
                            "PRIORITIZE_SALE" -> Icons.Default.TrendingUp
                            else -> Icons.Default.Star
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(typeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(getTypeLabel(rec.type), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                    }

                    // Prioridad
                    val priorityColor = when (rec.priority.uppercase()) {
                        "HIGH" -> Rose600
                        "MEDIUM" -> Amber600
                        else -> Emerald600
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getPriorityLabel(rec.priority), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = priorityColor)
                    }

                    // Detalle
                    Column(Modifier.weight(2.5f)) {
                        Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                        Text(rec.description, fontSize = 12.sp, color = Slate400)
                    }

                    // Sugerencia
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate100)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(rec.suggestedAction ?: "", fontSize = 13.sp, color = Slate600)
                    }

                    // Fecha
                    Text(
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                            try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(rec.createdAt) }
                            catch (e: Exception) { Date() }
                        ),
                        modifier = Modifier.weight(1.2f),
                        fontSize = 13.sp,
                        color = Slate400
                    )

                    // Estado
                    val statusColor = when (normStatus) {
                        "PENDING" -> Slate600
                        "ACCEPTED" -> Emerald600
                        "DISMISSED" -> Rose600
                        else -> Slate600
                    }
                    val statusBg = statusColor.copy(alpha = 0.1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getStatusDisplay(rec.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }

                    // Acciones
                    Box(Modifier.weight(1.5f), contentAlignment = Alignment.CenterEnd) {
                        if (normStatus == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onAccept(rec.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Aceptar", tint = Emerald600)
                                }
                                IconButton(
                                    onClick = { onDismiss(rec.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, "Descartar", tint = Rose600)
                                }
                            }
                        } else {
                            Icon(
                                if (normStatus == "ACCEPTED") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                null,
                                tint = if (normStatus == "ACCEPTED") Emerald600 else Rose600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(recs) { _, rec ->
                val normStatus = RecommendationsUiState.normalizeStatus(rec.status)
                val priorityColor = when (rec.priority.uppercase()) {
                    "HIGH" -> Rose600
                    "MEDIUM" -> Amber600
                    else -> Emerald600
                }
                ItemCard(
                    title = rec.title,
                    subtitle = "${getTypeLabel(rec.type)} · ${rec.description}",
                    badge = "${getPriorityLabel(rec.priority)} · ${getStatusDisplay(rec.status)}",
                    value = rec.suggestedAction ?: "",
                    valueColor = Slate900,
                    accentLeft = priorityColor,
                    actions = {
                        if (normStatus == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onAccept(rec.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Aceptar", tint = Emerald600)
                                }
                                IconButton(
                                    onClick = { onDismiss(rec.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, "Descartar", tint = Rose600)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// ── Componentes de tabla reutilizables (igual que Pérdidas) ──────────────────
private data class ColDef(val label: String, val weight: Float, val alignEnd: Boolean = false)

@Composable
private fun DataTable(
    headers: List<ColDef>,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    headers.forEach { col ->
                        Text(
                            col.label.uppercase(),
                            modifier = Modifier.weight(col.weight),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 0.6.sp,
                            textAlign = if (col.alignEnd) TextAlign.End else TextAlign.Start
                        )
                    }
                }
                Divider(color = Slate200)
            }
            content()
        }
    }
}

@Composable
private fun DataRow(even: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (even) White else Slate50.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
    Divider(color = Slate100)
}

// ── Item Card (modo vertical) ─────────────────────────────────────────────────
@Composable
private fun ItemCard(
    title: String,
    subtitle: String,
    badge: String,
    value: String,
    valueColor: Color,
    accentLeft: Color,
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentLeft)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Slate900,
                    modifier = Modifier.weight(1f)
                )
                if (actions != null) {
                    actions()
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = Slate400)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentLeft.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentLeft)
                }
                Spacer(Modifier.width(8.dp))
                Text(value, fontSize = 13.sp, color = valueColor, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Emerald50),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Emerald600, modifier = Modifier.size(32.dp))
            }
            Text(
                "No hay recomendaciones en esta vista.",
                color = Slate400,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}