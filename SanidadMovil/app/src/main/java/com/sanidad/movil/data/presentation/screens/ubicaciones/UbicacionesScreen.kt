package com.sanidad.movil.presentation.screens.ubicaciones

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.*

// Paleta fiel al diseño
private val Primary = Color(0xFF4F46E5)
private val PrimaryDark = Color(0xFF4338CA)
private val PrimaryLight = Color(0xFFEEF2FF)
private val Success = Color(0xFF10B981)
private val SuccessDark = Color(0xFF059669)
private val SuccessLight = Color(0xFFD1FAE5)
private val Danger = Color(0xFFEF4444)
private val DangerDark = Color(0xFFDC2626)
private val DangerLight = Color(0xFFFEE2E2)
private val Warning = Color(0xFFF59E0B)
private val WarningLight = Color(0xFFFEF3C7)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)
private val White = Color.White
private val CellBg = Color(0xFFFFFAF0)
private val CellBorder = Color(0xFFE2D5B6)

@Composable
fun UbicacionesScreen(viewModel: UbicacionesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()

    // ── Diálogo nuevo rack ──
    if (state.showNuevoRackDialog) {
        NuevoRackDialog(
            nuevoRack = state.nuevoRack,
            onCampoChange = { campo, valor -> viewModel.actualizarNuevoRack(campo, valor) },
            onGuardar = { viewModel.crearRack() },
            onCancelar = { viewModel.cerrarNuevoRackDialog() }
        )
    }

    // ── Banner de error ──
    AnimatedVisibility(visible = state.error != null) {
        state.error?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = DangerLight,
                border = BorderStroke(1.dp, Danger)
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

    // ── Banner modo movimiento ──
    AnimatedVisibility(visible = state.modoMovimiento) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(48.dp),
            color = Slate800.copy(alpha = 0.95f)
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✋ Modo movimiento activo", color = White, fontSize = 14.sp)
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.cancelarMovimiento() },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(40.dp)
                ) { Text("Cancelar") }
            }
        }
    }

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gestión de Ubicaciones", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                    Spacer(Modifier.width(12.dp))
                    Surface(shape = RoundedCornerShape(40.dp), color = PrimaryLight) {
                        Text(
                            "${state.racks.size} Estantes",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.cargarDatosBase() },
                        shape = RoundedCornerShape(40.dp)
                    ) { Text("↻ Recargar") }
                    if (isAdmin) {
                        Button(
                            onClick = { viewModel.abrirNuevoRackDialog() },
                            shape = RoundedCornerShape(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Success)
                        ) { Text("+ Nuevo Estante") }
                    }
                }
            }

            // ── Layout principal ──
            Row(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                // Panel izquierdo: lista de racks
                Card(
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.racks, key = { it.id }) { rack ->
                            val seleccionado = state.rackSeleccionado?.id == rack.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.seleccionarRack(rack.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (seleccionado) PrimaryLight else White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (seleccionado) Primary else Slate200
                                )
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(rack.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                        Text(
                                            "${rack.ancho}×${rack.alto}×${rack.profundidad}",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { viewModel.eliminarRack(rack.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Eliminar", tint = Danger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Área central: visualización del rack
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    if (state.rackSeleccionado == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text("Seleccione un estante del panel izquierdo", color = Slate400, fontSize = 16.sp)
                            }
                        }
                    } else {
                        RackVisualization(
                            rack = state.rackSeleccionado!!,
                            ubicaciones = state.ubicacionesRack,
                            celdaSeleccionada = state.celdaSeleccionada,
                            modoMovimiento = state.modoMovimiento,
                            origenMovimiento = state.origenMovimiento,
                            onCeldaClick = { n, c, p -> viewModel.onCeldaClick(n, c, p) }
                        )
                    }
                }

                // Panel derecho: asignación / info / movimiento
                AnimatedVisibility(
                    visible = state.showAsignarPanel || state.modoMovimiento,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        if (state.modoMovimiento && state.origenMovimiento != null) {
                            // Panel de movimiento
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Modo Movimiento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Origen: ${state.origenMovimiento!!.medicamentoNombre}")
                                Text("Cantidad: ${state.origenMovimiento!!.cantidad}")
                                Spacer(Modifier.height(8.dp))
                                Text("Haga clic en una celda libre para mover.", color = Slate500, fontSize = 13.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.cancelarMovimiento() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                                ) { Text("Cancelar") }
                            }
                        } else if (state.infoCelda != null) {
                            // Celda ocupada → info
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📦", fontSize = 48.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Text(state.infoCelda!!.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Lote: ${state.infoCelda!!.factura ?: "—"}")
                                Text("Vence: ${state.infoCelda!!.vence ?: "—"}")
                                Spacer(Modifier.height(16.dp))
                                if (isAdmin) {
                                    Button(
                                        onClick = {
                                            val celda = state.celdaSeleccionada!!
                                            viewModel.onCeldaClick(celda.nivel, celda.columna, celda.profundidadIndex)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) { Text("Mover a otra celda") }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.eliminarUbicacion(state.infoCelda!!.ubicacionId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                                    ) { Text("Eliminar") }
                                }
                            }
                        } else {
                            // Celda libre → asignar
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Asignar producto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { viewModel.cerrarPanelAsignar() }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = state.searchTerm,
                                    onValueChange = { viewModel.setSearchTerm(it) },
                                    placeholder = { Text("Buscar lote/medicina...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(state.lotesConStockReal, key = { it.lote.id }) { lc ->
                                        Column {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Slate50,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "LOTE: ${lc.lote.factura}",
                                                    modifier = Modifier.padding(8.dp),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Slate600
                                                )
                                            }
                                            lc.detallesDisponibles.forEach { det ->
                                                val med = state.medicamentos.find { it.id == det.medicamentoId }
                                                val seleccionado = state.detalleSeleccionado?.id == det.id
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.setDetalleSeleccionado(det) },
                                                    color = if (seleccionado) PrimaryLight else White,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = if (seleccionado) BorderStroke(2.dp, Primary) else null
                                                ) {
                                                    Row(
                                                        Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            med?.nombre ?: "Desconocido",
                                                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(20.dp),
                                                            color = Slate100
                                                        ) {
                                                            Text(
                                                                "${det.cantidad} disponibles",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (state.detalleSeleccionado != null) {
                                    Spacer(Modifier.height(12.dp))
                                    // Stepper cantidad
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.setCantidad(maxOf(1, state.cantidad - 1)) }) {
                                            Icon(Icons.Default.Remove, null)
                                        }
                                        Text(
                                            "${state.cantidad}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        IconButton(onClick = {
                                            viewModel.setCantidad(
                                                minOf(state.detalleSeleccionado!!.cantidad, state.cantidad + 1)
                                            )
                                        }) {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.asignarEnCascada() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Success)
                                    ) { Text("Ubicar ${state.cantidad} uds.") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── VISUALIZACIÓN DEL RACK ──
@Composable
private fun RackVisualization(
    rack: RackResponse,
    ubicaciones: List<UbicacionLoteResponse>,
    celdaSeleccionada: Celda?,
    modoMovimiento: Boolean,
    origenMovimiento: OrigenMovimiento?,
    onCeldaClick: (Int, Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) { Text("📦", fontSize = 24.sp) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(rack.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate800)
                Text(
                    "${rack.alto} niveles · ${rack.ancho} columnas · ${rack.profundidad} fondo",
                    fontSize = 13.sp,
                    color = Slate500
                )
            }
        }

        // Leyenda
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            LegendItem(color = Slate200, text = "Libre", isDashed = true)
            LegendItem(color = Slate300, text = "Ocupado")
            if (celdaSeleccionada != null) LegendItem(color = Primary, text = "Selección")
            if (modoMovimiento) LegendItem(color = SuccessLight, text = "Origen")
        }

        // Grid 3D del rack (niveles de arriba hacia abajo)
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            for (nivelVisual in (rack.alto - 1) downTo 0) {
                val nivelIdx = nivelVisual
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Etiqueta de nivel
                    Text(
                        "Nv.${nivelVisual + 1}",
                        modifier = Modifier.width(40.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    // Columnas
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0 until rack.ancho) {
                            // Profundidades apiladas (efecto 3D)
                            Box(
                                modifier = Modifier.width(56.dp).height(56.dp)
                            ) {
                                for (prof in 0 until rack.profundidad) {
                                    val ocupada = ubicaciones.firstOrNull {
                                        it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                    }
                                    val esSeleccionada = celdaSeleccionada?.let {
                                        it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                    } ?: false
                                    val esOrigen = modoMovimiento && origenMovimiento?.let {
                                        it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                    } ?: false

                                    val offsetX = (prof * 6).dp
                                    val offsetY = (-prof * 6).dp

                                    Box(
                                        modifier = Modifier
                                            .offset(x = offsetX, y = offsetY)
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    esSeleccionada -> Primary
                                                    esOrigen -> SuccessLight
                                                    ocupada != null -> Slate200
                                                    else -> White
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    esSeleccionada || esOrigen -> 2.dp
                                                    ocupada != null -> 1.dp
                                                    else -> 1.dp
                                                },
                                                color = when {
                                                    esSeleccionada -> PrimaryDark
                                                    esOrigen -> Success
                                                    ocupada != null -> Slate300
                                                    else -> Slate300.copy(alpha = 0.5f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onCeldaClick(nivelIdx, col, prof) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (esOrigen) {
                                            Text("↕", fontSize = 16.sp)
                                        } else if (ocupada != null) {
                                            Text("📦", fontSize = 14.sp)
                                        }
                                        // Número de profundidad
                                        Text(
                                            "$prof",
                                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                            fontSize = 9.sp,
                                            color = Slate500,
                                            fontWeight = FontWeight.Bold
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
}

@Composable
private fun LegendItem(color: Color, text: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(4.dp))
                .then(if (isDashed) Modifier.border(1.dp, Slate300, RoundedCornerShape(4.dp)) else Modifier)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = Slate600)
    }
}

// ── DIÁLOGO NUEVO RACK ──
@Composable
private fun NuevoRackDialog(
    nuevoRack: NuevoRackState,
    onCampoChange: (String, Any) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo Estante", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = nuevoRack.nombre,
                    onValueChange = { onCampoChange("nombre", it) },
                    label = { Text("Nombre del Estante") },
                    placeholder = { Text("Ej: Pasillo A - Estante 1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(16.dp))
                DimensionStepper("Niveles (alto)", nuevoRack.alto) { onCampoChange("alto", it) }
                DimensionStepper("Columnas (ancho)", nuevoRack.ancho) { onCampoChange("ancho", it) }
                DimensionStepper("Fondo", nuevoRack.profundidad) { onCampoChange("profundidad", it) }
            }
        },
        confirmButton = {
            Button(
                onClick = onGuardar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) { Text("Guardar Estante") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DimensionStepper(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Slate50)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium, color = Slate700)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange(maxOf(1, value - 1)) },
                modifier = Modifier.size(32.dp)
            ) { Icon(Icons.Default.Remove, null) }
            Text(
                "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(32.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    }
}