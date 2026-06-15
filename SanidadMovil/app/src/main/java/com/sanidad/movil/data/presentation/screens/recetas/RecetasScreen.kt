package com.sanidad.movil.presentation.screens.recetas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import java.text.SimpleDateFormat
import java.util.Locale

// Paleta unificada
private val Primary = Color(0xFF4F46E5)
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
fun RecetasScreen(viewModel: RecetasViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val esAdmin = UserSession.rol == "ADMIN"
    val esFarmaceutico = UserSession.rol == "FARMACEUTICO"
    val farmaceuticoId = UserSession.userId ?: 0L
    val puedeValidar = esAdmin || esFarmaceutico

    LaunchedEffect(state.mostrarTodas) {
        viewModel.cargarRecetas(esAdmin, farmaceuticoId)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }

    if (state.showValidarDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.ocultarValidarDialog() },
            title = { Text(if (state.aprobarAction) "Aprobar receta" else "Rechazar receta") },
            text = { Text(if (state.aprobarAction) "¿Desea aprobar esta receta?" else "¿Desea rechazar esta receta?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarValidacion(farmaceuticoId) }) {
                    Text(if (state.aprobarAction) "Aprobar" else "Rechazar")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.ocultarValidarDialog() }) { Text("Cancelar") } }
        )
    }

    Scaffold(containerColor = Slate50) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header unificado (sin botón duplicado)
            RecetasHeader(
                mostrarTodas = state.mostrarTodas,
                esAdmin = esAdmin,
                onToggleMostrarTodas = { viewModel.toggleMostrarTodas() }
            )

            Spacer(Modifier.height(16.dp))

            // Tarjeta de subida (único lugar para subir)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📤 Subir nueva receta", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Slate900)
                    Text("Formato permitido: JPG, PNG", fontSize = 12.sp, color = Slate500)
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selector de imagen como OutlinedButton
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp), tint = Slate500)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.selectedImageUri?.let { uri -> uri.lastPathSegment ?: "Imagen seleccionada" } ?: "Seleccionar imagen",
                                color = if (state.selectedImageUri != null) Slate700 else Slate400,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        OutlinedTextField(
                            value = state.codigoMinsa,
                            onValueChange = { viewModel.setCodigoMinsa(it) },
                            placeholder = { Text("Código MINSA") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Slate200,
                                focusedBorderColor = Primary
                            )
                        )

                        Button(
                            onClick = { viewModel.subirReceta(farmaceuticoId) },
                            enabled = state.selectedImageUri != null && state.codigoMinsa.isNotBlank() && !state.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                disabledContainerColor = Slate300
                            ),
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Subir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    AnimatedVisibility(visible = state.uploadError != null) {
                        state.uploadError?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(error, color = Danger, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Listado de recetas (ocupa el resto de la pantalla)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    when {
                        state.isLoading && state.recetas.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Primary)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Cargando recetas...", color = Slate500, fontSize = 14.sp)
                                }
                            }
                        }
                        state.error != null && state.recetas.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(state.error!!, color = Danger, fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.cargarRecetas(esAdmin, farmaceuticoId) }) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                        }
                        state.recetas.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No se encontraron recetas.", color = Slate400, fontSize = 14.sp)
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.recetas, key = { it.id }) { receta ->
                                    RecetaCard(
                                        receta = receta,
                                        puedeValidar = puedeValidar,
                                        onAprobar = { viewModel.mostrarValidarDialog(receta.id, true) },
                                        onRechazar = { viewModel.mostrarValidarDialog(receta.id, false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──── Header con toggle de vista ────
@Composable
private fun RecetasHeader(
    mostrarTodas: Boolean,
    esAdmin: Boolean,
    onToggleMostrarTodas: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Gestión de Recetas", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
            Text("Validación y control de recetas médicas", fontSize = 14.sp, color = Slate500)
        }
        // Toggle de vista como FilterChip
        FilterChip(
            selected = mostrarTodas,
            onClick = onToggleMostrarTodas,
            label = {
                Text(
                    if (mostrarTodas) "📋 Todas" else if (esAdmin) "🧾 Pendientes" else "📚 Mías",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary.copy(alpha = 0.15f),
                selectedLabelColor = Primary
            )
        )
    }
}

// ──── Tarjeta de receta mejorada ────
@Composable
private fun RecetaCard(
    receta: com.sanidad.movil.data.remote.dto.RecetaResponse,
    puedeValidar: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                if (receta.imagenUrl != null) {
                    AsyncImage(
                        model = "http://172.16.66.6:8080${receta.imagenUrl}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null, tint = Slate400, modifier = Modifier.size(48.dp))
                    }
                }
                val badgeColor = when (receta.estado) {
                    "PENDIENTE" -> Warning
                    "APROBADA" -> Success
                    "RECHAZADA" -> Danger
                    else -> Slate500
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor
                ) {
                    Text(
                        receta.estado,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Farmacéutico: ${receta.farmaceuticoUsername ?: ""}",
                    fontSize = 13.sp,
                    color = Slate700
                )
                Text(
                    "Fecha: ${receta.fechaSubida?.let { formatearFecha(it) } ?: ""}",
                    fontSize = 12.sp,
                    color = Slate500
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Código MINSA:", fontSize = 12.sp, color = Slate600)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100
                    ) {
                        Text(
                            receta.codigoMinsa ?: "N/A",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                }
                if (receta.ventaId != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("🔗 Venta #${receta.ventaId}", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
            }

            if (puedeValidar && receta.estado == "PENDIENTE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAprobar,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Aprobar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = White)
                    }
                    Button(
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = White)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

fun formatearFecha(fecha: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        date?.let { outputFormat.format(it) } ?: fecha
    } catch (e: Exception) {
        fecha
    }
}