package com.sanidad.movil.presentation.screens.perdidas

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
import com.sanidad.movil.data.remote.dto.*

// ── Paleta refinada ──────────────────────────────────────────────────────────
private val Indigo950  = Color(0xFF1E1B4B)   // Texto principal / header
private val Indigo600  = Color(0xFF4F46E5)   // Acento tab activo
private val Indigo100  = Color(0xFFE0E7FF)   // Fondo tab activo
private val Emerald600 = Color(0xFF059669)   // Éxito / recarga
private val Emerald50  = Color(0xFFECFDF5)
private val Rose600    = Color(0xFFDC2626)   // Peligro / vencido
private val Rose50     = Color(0xFFFFF1F2)
private val Amber600   = Color(0xFFD97706)   // Advertencia / inconsistencia
private val Amber50    = Color(0xFFFFFBEB)
private val Slate900   = Color(0xFF0F172A)
private val Slate600   = Color(0xFF475569)
private val Slate400   = Color(0xFF94A3B8)
private val Slate200   = Color(0xFFE2E8F0)
private val Slate100   = Color(0xFFF1F5F9)
private val Slate50    = Color(0xFFF8FAFC)
private val White      = Color.White

// ── Tabs config ──────────────────────────────────────────────────────────────
private data class TabItem(val label: String, val icon: ImageVector, val index: Int)
private val TABS = listOf(
    TabItem("Vencidos",       Icons.Default.Warning,   0),
    TabItem("Sin Rotación",   Icons.Default.Inventory, 1),
    TabItem("Inconsistencias",Icons.Default.BarChart,  2),
)

// ── Pantalla principal ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerdidasScreen(viewModel: PerdidasViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val wideMode = LocalConfiguration.current.screenWidthDp > 600

    Scaffold(containerColor = Slate50) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header ──
            PerdidasHeader(onRefresh = { viewModel.cargarDatos() })

            // ── KPI Row ──
            PerdidasKpiRow(
                vencimiento  = viewModel.formatCurrency(state.resumen?.totalPerdidasVencimiento ?: 0.0),
                cantVencidos = state.resumen?.cantidadProductosVencidos ?: 0,
                inmovilizado = viewModel.formatCurrency(state.resumen?.totalInmovilizado ?: 0.0),
                cantInmoviles= state.resumen?.cantidadProductosInmoviles ?: 0,
                inconsistencias = state.resumen?.cantidadInconsistencias ?: 0
            )

            // ── Error banner ──
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { ErrorBanner(message = it) { viewModel.cargarDatos() } }
            }

            Spacer(Modifier.height(20.dp))

            // ── Segmented tab control ──
            SegmentedTabBar(
                tabs       = TABS,
                activeIndex= state.activeTab,
                onSelect   = { viewModel.setActiveTab(it) },
                modifier   = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Contenido ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                when {
                    state.isLoading -> CenteredLoader()
                    else -> AnimatedContent(
                        targetState = state.activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith
                                    fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                        }
                    ) { tab ->
                        when (tab) {
                            0 -> VencidosContent(state.vencidos, viewModel::formatCurrency, wideMode)
                            1 -> InmovilesContent(state.inmoviles, viewModel::formatCurrency, wideMode)
                            2 -> InconsistenciasContent(state.inconsistencias, wideMode)
                        }
                    }
                }
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────
@Composable
private fun PerdidasHeader(onRefresh: () -> Unit) {
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
                "Control de Pérdidas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Indigo950,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Mermas · Stock inmovilizado · Discrepancias",
                fontSize = 13.sp,
                color = Slate400,
                letterSpacing = 0.sp
            )
        }

        // Botón minimalista solo ícono
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald50)
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Recargar",
                tint = Emerald600,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Divider(color = Slate200, thickness = 1.dp)
}

// ── KPI Cards ─────────────────────────────────────────────────────────────────
@Composable
private fun PerdidasKpiRow(
    vencimiento: String, cantVencidos: Int,
    inmovilizado: String, cantInmoviles: Int,
    inconsistencias: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiCard(
            label    = "Vencimiento",
            value    = vencimiento,
            detail   = "$cantVencidos productos",
            accentColor = Rose600,
            bgColor  = Rose50,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label    = "Inmovilizado",
            value    = inmovilizado,
            detail   = "$cantInmoviles SKU",
            accentColor = Slate600,
            bgColor  = Slate100,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            label    = "Inconsistencias",
            value    = inconsistencias.toString(),
            detail   = "diferencias",
            accentColor = Amber600,
            bgColor  = Amber50,
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
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
                color = Slate900,
                letterSpacing = (-0.3).sp
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

// ── Segmented Tab Bar ─────────────────────────────────────────────────────────
@Composable
private fun SegmentedTabBar(
    tabs: List<TabItem>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
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
            val isActive = tab.index == activeIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isActive) White else Color.Transparent)
                    .clickable { onSelect(tab.index) }
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

// ── Contenido Vencidos ───────────────────────────────────────────────────────
@Composable
private fun VencidosContent(
    vencidos: List<ProductoVencidoDTO>,
    formatCurrency: (Double) -> String,
    wideMode: Boolean
) {
    if (vencidos.isEmpty()) {
        EmptyState("Sin productos vencidos con stock activo.", Icons.Default.CheckCircle, Emerald600)
        return
    }
    if (wideMode) {
        DataTable(
            headers = listOf(
                ColDef("Lote", 1f),
                ColDef("Medicamento", 2f),
                ColDef("Vencimiento", 1f),
                ColDef("Cantidad", 1f),
                ColDef("Valor perdido", 1f, alignEnd = true)
            )
        ) {
            itemsIndexed(vencidos) { i, v ->
                DataRow(even = i % 2 == 0) {
                    DataCell(v.numeroLote ?: "—", 1f, bold = true)
                    DataCell(v.medicamentoNombre, 2f)
                    DataCell(v.fechaVencimiento, 1f, color = Slate400)
                    DataCell("${v.cantidadVencida} u.", 1f)
                    DataCell(formatCurrency(v.valorPerdido), 1f, bold = true, color = Rose600, alignEnd = true)
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(vencidos) { _, v ->
                ItemCard(
                    title    = v.medicamentoNombre,
                    subtitle = "Lote ${v.numeroLote ?: "—"}  ·  Vence ${v.fechaVencimiento}",
                    badge    = "${v.cantidadVencida} u.",
                    value    = formatCurrency(v.valorPerdido),
                    valueColor = Rose600,
                    accentLeft = Rose600
                )
            }
        }
    }
}

// ── Contenido Inmóviles ───────────────────────────────────────────────────────
@Composable
private fun InmovilesContent(
    inmoviles: List<ProductoInmovilDTO>,
    formatCurrency: (Double) -> String,
    wideMode: Boolean
) {
    if (inmoviles.isEmpty()) {
        EmptyState("Todos los productos tienen rotación activa.", Icons.Default.CheckCircle, Emerald600)
        return
    }
    if (wideMode) {
        DataTable(
            headers = listOf(
                ColDef("Medicamento", 2f),
                ColDef("Stock", 1f),
                ColDef("Días sin mov.", 1f),
                ColDef("Valor inmov.", 1f, alignEnd = true)
            )
        ) {
            itemsIndexed(inmoviles) { i, p ->
                DataRow(even = i % 2 == 0) {
                    DataCell(p.medicamentoNombre, 2f, bold = true)
                    DataCell("${p.stockActual} u.", 1f)
                    DataCell("${p.diasSinMovimiento} días", 1f, color = Amber600)
                    DataCell(formatCurrency(p.valorInmovilizado), 1f, bold = true, alignEnd = true)
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(inmoviles) { _, p ->
                ItemCard(
                    title    = p.medicamentoNombre,
                    subtitle = "Stock: ${p.stockActual} u.  ·  ${p.diasSinMovimiento} días sin movimiento",
                    badge    = null,
                    value    = formatCurrency(p.valorInmovilizado),
                    valueColor = Slate900,
                    accentLeft = Amber600
                )
            }
        }
    }
}

// ── Contenido Inconsistencias ─────────────────────────────────────────────────
@Composable
private fun InconsistenciasContent(
    inconsistencias: List<InconsistenciaStockDTO>,
    wideMode: Boolean
) {
    if (inconsistencias.isEmpty()) {
        EmptyState("Integridad de stock verificada.", Icons.Default.CheckCircle, Emerald600)
        return
    }
    if (wideMode) {
        DataTable(
            headers = listOf(
                ColDef("Medicamento", 2f),
                ColDef("Stock lote", 1f),
                ColDef("Stock ubic.", 1f),
                ColDef("Diferencia", 1f, alignEnd = true)
            )
        ) {
            itemsIndexed(inconsistencias) { i, inc ->
                val diffColor = if (inc.diferencia > 0) Amber600 else Rose600
                DataRow(even = i % 2 == 0) {
                    DataCell(inc.medicamentoNombre, 2f, bold = true)
                    DataCell("${inc.cantidadLote}", 1f)
                    DataCell("${inc.cantidadUbicaciones}", 1f)
                    DataCell(
                        if (inc.diferencia > 0) "+${inc.diferencia}" else "${inc.diferencia}",
                        1f, bold = true, color = diffColor, alignEnd = true
                    )
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(inconsistencias) { _, inc ->
                val diffColor = if (inc.diferencia > 0) Amber600 else Rose600
                ItemCard(
                    title    = inc.medicamentoNombre,
                    subtitle = "Lote: ${inc.cantidadLote}  ·  Ubicación: ${inc.cantidadUbicaciones}",
                    badge    = "Dif.",
                    value    = if (inc.diferencia > 0) "+${inc.diferencia}" else "${inc.diferencia}",
                    valueColor = diffColor,
                    accentLeft = diffColor
                )
            }
        }
    }
}

// ── Componentes de tabla reutilizables ────────────────────────────────────────
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
            // Header
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

@Composable
private fun RowScope.DataCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    color: Color = Slate900,
    alignEnd: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

// ── Item Card (modo vertical) ─────────────────────────────────────────────────
@Composable
private fun ItemCard(
    title: String,
    subtitle: String,
    badge: String?,
    value: String,
    valueColor: Color,
    accentLeft: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
    ) {
        // Borde izquierdo de color (firma visual)
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentLeft)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Slate900
                )
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.sp, color = Slate400)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (badge != null) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(valueColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = valueColor)
                    }
                }
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(message: String, icon: ImageVector, iconColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp))
            }
            Text(
                message,
                color = Slate400,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}