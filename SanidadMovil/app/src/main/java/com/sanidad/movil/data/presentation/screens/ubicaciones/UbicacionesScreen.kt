package com.sanidad.movil.data.presentation.screens.ubicaciones

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbicacionesScreen(viewModel: UbicacionesViewModel = viewModel()) {
    val racks by viewModel.racks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val rackSeleccionado by viewModel.rackSeleccionado.collectAsState()
    val ubicacionesRack by viewModel.ubicacionesRack.collectAsState()
    val celdaSeleccionada by viewModel.celdaSeleccionada.collectAsState()
    val infoCelda by viewModel.infoCelda.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val lotesConStockReal by viewModel.lotesConStockReal.collectAsState()
    val detalleSeleccionado by viewModel.detalleSeleccionado.collectAsState()
    val cantidad by viewModel.cantidad.collectAsState()
    val modoMovimiento by viewModel.modoMovimiento.collectAsState()
    val origenMovimiento by viewModel.origenMovimiento.collectAsState()
    val showNuevoRackDialog by viewModel.showNuevoRackDialog.collectAsState()
    val nuevoRack by viewModel.nuevoRack.collectAsState()

    val isAdmin = UserSession.isAdmin()

    LaunchedEffect(Unit) { viewModel.cargarDatosBase() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ubicaciones") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { viewModel.abrirNuevoRackDialog() }) { // ✅
                    Icon(Icons.Default.Add, "Nuevo estante")
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Panel izquierdo: lista de racks
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                racks.forEach { rack ->
                    val seleccionado = rackSeleccionado?.id == rack.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.seleccionarRack(rack.id) }
                            .padding(4.dp),
                        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rack.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${rack.ancho}x${rack.alto}x${rack.profundidad}", fontSize = 11.sp)
                            }
                            if (isAdmin) {
                                IconButton(onClick = { viewModel.eliminarRack(rack.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Área principal: visualización del rack
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (rackSeleccionado != null) {
                    // Encabezado
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(rackSeleccionado!!.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("${rackSeleccionado!!.alto} niveles · ${rackSeleccionado!!.ancho} columnas · ${rackSeleccionado!!.profundidad} fondo")
                        }
                    }

                    // Leyenda
                    Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(color = Color(0xFFBDBDBD), text = "Libre")
                        LegendItem(color = Color(0xFF81C784), text = "Ocupado")
                        if (modoMovimiento) LegendItem(color = Color(0xFFFF8A65), text = "Origen")
                    }

                    // Grid de celdas
                    LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                        for (nivelVisual in (rackSeleccionado!!.alto - 1) downTo 0) {
                            val nivelIdx = nivelVisual
                            item {
                                Text("Nivel ${nivelVisual + 1}", modifier = Modifier.padding(vertical = 4.dp), fontWeight = FontWeight.Bold)
                                Row {
                                    for (col in 0 until rackSeleccionado!!.ancho) {
                                        Column {
                                            for (prof in 0 until rackSeleccionado!!.profundidad) {
                                                val ocupada = ubicacionesRack.find {
                                                    it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                                }
                                                val esSeleccionada = celdaSeleccionada?.let {
                                                    it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                                } ?: false
                                                val esOrigen = modoMovimiento && origenMovimiento?.let {
                                                    it.nivel == nivelIdx && it.columna == col && it.profundidadIndex == prof
                                                } ?: false

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when {
                                                                esOrigen -> Color(0xFFFF8A65)
                                                                ocupada != null -> Color(0xFF81C784)
                                                                else -> Color(0xFFBDBDBD)
                                                            }
                                                        )
                                                        .clickable { viewModel.onCeldaClick(nivelIdx, col, prof) }
                                                        .border(
                                                            width = if (esSeleccionada) 2.dp else 0.dp,
                                                            color = Color.Blue,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("$prof", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Panel derecho
                    if (celdaSeleccionada != null || modoMovimiento) {
                        Surface(
                            modifier = Modifier
                                .width(200.dp)
                                .fillMaxHeight()
                                .padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (infoCelda != null && !modoMovimiento) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(infoCelda!!.medicamentoNombre, fontWeight = FontWeight.Bold)
                                    Text("Lote: ${infoCelda!!.factura}")
                                    Text("Vence: ${infoCelda!!.vence}")
                                    if (isAdmin) {
                                        Button(
                                            onClick = {
                                                val celda = celdaSeleccionada!!
                                                viewModel.onCeldaClick(celda.nivel, celda.columna, celda.profundidadIndex)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Mover")
                                        }
                                        TextButton(onClick = { viewModel.eliminarUbicacion(infoCelda!!.ubicacionId) }) {
                                            Text("Eliminar", color = Color.Red)
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Asignar producto", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(
                                        value = searchTerm,
                                        onValueChange = { viewModel.setSearchTerm(it) }, // ✅
                                        label = { Text("Buscar lote/medicina") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(lotesConStockReal) { loteCon ->
                                            Column {
                                                Text("LOTE: ${loteCon.lote.factura}", fontWeight = FontWeight.Bold)
                                                loteCon.detallesDisponibles.forEach { det ->
                                                    val med = viewModel.medicamentos.value.find { it.id == det.medicamentoId }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { viewModel.setDetalleSeleccionado(det) } // ✅
                                                            .padding(4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(med?.nombre ?: "Desconocido", modifier = Modifier.weight(1f))
                                                        Text("${det.cantidad} uds")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (detalleSeleccionado != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.setCantidad(maxOf(1, cantidad - 1)) }) { Text("-") } // ✅
                                            Text("$cantidad")
                                            IconButton(onClick = { viewModel.setCantidad(minOf(detalleSeleccionado!!.cantidad, cantidad + 1)) }) { Text("+") } // ✅
                                        }
                                        Button(onClick = { viewModel.asignarEnCascada() }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Ubicar $cantidad uds.")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (modoMovimiento) {
                        Surface(
                            modifier = Modifier
                                .width(200.dp)
                                .fillMaxHeight()
                                .padding(start = 8.dp),
                            color = Color(0xFFFFF9C4)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Modo movimiento", fontWeight = FontWeight.Bold)
                                origenMovimiento?.let {
                                    Text(it.medicamentoNombre)
                                    Text("Cantidad: ${it.cantidad}")
                                }
                                Button(onClick = { viewModel.cancelarMovimiento() }) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Seleccione un estante del panel izquierdo")
                    }
                }
            }
        }
    }

    // Diálogo nuevo rack
    if (showNuevoRackDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarNuevoRackDialog() }, // ✅
            title = { Text("Nuevo Estante") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nuevoRack.nombre,
                        onValueChange = { viewModel.actualizarNuevoRack("nombre", it) },
                        label = { Text("Nombre") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DimensionStepper("Niveles (alto)", nuevoRack.alto) { viewModel.actualizarNuevoRack("alto", it) }
                    DimensionStepper("Columnas (ancho)", nuevoRack.ancho) { viewModel.actualizarNuevoRack("ancho", it) }
                    DimensionStepper("Fondo", nuevoRack.profundidad) { viewModel.actualizarNuevoRack("profundidad", it) }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.crearRack(onSuccess = { viewModel.cerrarNuevoRackDialog() }) }) { Text("Guardar") } // ✅
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cerrarNuevoRackDialog() }) { Text("Cancelar") } // ✅
            }
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun DimensionStepper(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onValueChange(maxOf(1, value - 1)) }) { Text("-") }
        Text("$value")
        IconButton(onClick = { onValueChange(value + 1) }) { Text("+") }
    }
}