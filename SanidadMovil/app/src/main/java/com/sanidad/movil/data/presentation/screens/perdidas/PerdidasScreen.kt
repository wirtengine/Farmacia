package com.sanidad.movil.presentation.screens.perdidas

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.remote.dto.*

// Paleta fiel al CSS original
private val Primary = Color(0xFF10B981)        // éxito / verde principal
private val Danger = Color(0xFFEF4444)          // rojo para pérdidas
private val Warning = Color(0xFFF59E0B)         // naranja discrepancias
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White

@Composable
fun PerdidasScreen(viewModel: PerdidasViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header elegante
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    "Control de Pérdidas y Análisis Operativo",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Visualización centralizada de mermas, stock inmovilizado y discrepancias de inventario.",
                    fontSize = 14.sp,
                    color = Slate500
                )
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                state.error != null -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(state.error!!, color = Danger, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.cargarDatos() }) { Text("Reintentar") }
                    }
                }
                else -> {
                    // === KPIs ===
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            title = "Pérdidas por Vencimiento",
                            value = viewModel.formatCurrency(state.resumen?.totalPerdidasVencimiento ?: 0.0),
                            subtitle = "${state.resumen?.cantidadProductosVencidos ?: 0} productos vencidos",
                            icon = Icons.Default.Warning,
                            iconBg = Color(0xFFFFF1F2),
                            iconColor = Danger,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Inventario Inmovilizado",
                            value = viewModel.formatCurrency(state.resumen?.totalInmovilizado ?: 0.0),
                            subtitle = "${state.resumen?.cantidadProductosInmoviles ?: 0} SKU sin rotación",
                            icon = Icons.Default.Inventory,
                            iconBg = Slate100,
                            iconColor = Slate500,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Inconsistencias",
                            value = "${state.resumen?.cantidadInconsistencias ?: 0}",
                            subtitle = "Diferencias detectadas en stock",
                            icon = Icons.Default.BarChart,
                            iconBg = Color(0xFFFFFBEB),
                            iconColor = Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // === Pestañas en chips ===
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .background(Slate100, RoundedCornerShape(40.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Productos Vencidos" to 0,
                            "Sin Rotación" to 1,
                            "Inconsistencias" to 2
                        ).forEach { (label, index) ->
                            val isActive = state.activeTab == index
                            TextButton(
                                onClick = { viewModel.setActiveTab(index) },
                                shape = RoundedCornerShape(40.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (isActive) White else Color.Transparent,
                                    contentColor = if (isActive) Primary else Slate500
                                ),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // === Contenido de la pestaña activa ===
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        when (state.activeTab) {
                            0 -> VencidosTab(state.vencidos, viewModel::formatCurrency)
                            1 -> InmovilesTab(state.inmoviles, viewModel::formatCurrency)
                            2 -> InconsistenciasTab(state.inconsistencias)
                        }
                    }
                }
            }
        }
    }
}

// ---------- KPI Card (CORREGIDO: se eliminó textTransform) ----------
@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                // CORRECCIÓN: .uppercase() en lugar de textTransform
                Text(
                    title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Spacer(Modifier.height(2.dp))
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                Text(subtitle, fontSize = 12.sp, color = Slate400)
            }
        }
    }
}

// ---------- Vencidos ----------
@Composable
private fun VencidosTab(vencidos: List<ProductoVencidoDTO>, formatCurrency: (Double) -> String) {
    if (vencidos.isEmpty()) {
        EmptyState("No se registran productos vencidos con stock.")
    } else {
        LazyColumn {
            itemsIndexed(vencidos) { _, v ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Text(
                                    v.numeroLote ?: "",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(v.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Vence: ${v.fechaVencimiento}", fontSize = 12.sp, color = Slate500)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${v.cantidadVencida} u.", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                formatCurrency(v.valorPerdido),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Danger
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Inmoviles ----------
@Composable
private fun InmovilesTab(inmoviles: List<ProductoInmovilDTO>, formatCurrency: (Double) -> String) {
    if (inmoviles.isEmpty()) {
        EmptyState("Todos los productos presentan rotación activa.")
    } else {
        LazyColumn {
            itemsIndexed(inmoviles) { _, p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Stock: ${p.stockActual} u.", fontSize = 12.sp, color = Slate500)
                            Text("Días sin movimiento: ${p.diasSinMovimiento}", fontSize = 12.sp, color = Slate500)
                        }
                        Text(
                            formatCurrency(p.valorInmovilizado),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                }
            }
        }
    }
}

// ---------- Inconsistencias ----------
@Composable
private fun InconsistenciasTab(inconsistencias: List<InconsistenciaStockDTO>) {
    if (inconsistencias.isEmpty()) {
        EmptyState("Integridad de stock verificada.")
    } else {
        LazyColumn {
            itemsIndexed(inconsistencias) { _, inc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inc.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Stock lote: ${inc.cantidadLote}", fontSize = 12.sp, color = Slate500)
                            Text("Stock ubicación: ${inc.cantidadUbicaciones}", fontSize = 12.sp, color = Slate500)
                        }
                        Text(
                            if (inc.diferencia > 0) "+${inc.diferencia}" else "${inc.diferencia}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inc.diferencia > 0) Warning else Danger
                        )
                    }
                }
            }
        }
    }
}

// ---------- Estado vacío ----------
@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(message, color = Slate500, style = MaterialTheme.typography.bodyLarge)
        }
    }
}