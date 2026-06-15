package com.sanidad.movil.presentation.screens.racks

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

// Paleta fiel al diseño general
private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White

@Composable
fun RacksScreen(viewModel: RacksViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            RacksHeader(
                onRefresh = { viewModel.cargarRacks() },
                isLoading = state.isLoading
            )

            // Error
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Danger)
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = Color(0xFF991B1B))
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.limpiarError() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = Danger)
                            }
                        }
                    }
                }
            }

            // Lista de racks
            if (state.isLoading && state.racks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (state.racks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron estantes.", color = Slate400)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    itemsIndexed(state.racks, key = { _, r -> r.id }) { _, rack ->
                        RackCard(rack = rack)
                    }
                }
            }
        }
    }
}

@Composable
private fun RacksHeader(onRefresh: () -> Unit, isLoading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Estantes (Racks)",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            Text(
                "Vista general de todos los estantes del sistema",
                fontSize = 14.sp,
                color = Slate500
            )
        }
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Primary)
        }
    }
}

@Composable
private fun RackCard(rack: com.sanidad.movil.data.remote.dto.RackResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del rack
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warehouse,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rack.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Slate900
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dimensiones
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100
                    ) {
                        Text(
                            text = "${rack.ancho}×${rack.alto}×${rack.profundidad}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                    // Capacidad total
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100
                    ) {
                        Text(
                            text = "${rack.ancho * rack.alto * rack.profundidad} celdas",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                }
                // Estado
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (rack.activo) Success else Slate400)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (rack.activo) "Activo" else "Inactivo",
                        fontSize = 13.sp,
                        color = if (rack.activo) Success else Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Descripción (si existe)
            if (!rack.descripcion.isNullOrBlank()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = rack.descripcion,
                    fontSize = 12.sp,
                    color = Slate500,
                    maxLines = 2
                )
            }
        }
    }
}