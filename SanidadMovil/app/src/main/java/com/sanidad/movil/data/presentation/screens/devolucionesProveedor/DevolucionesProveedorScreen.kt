package com.sanidad.movil.data.presentation.screens.devolucionesProveedor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionesProveedorScreen(viewModel: DevolucionesProveedorViewModel = viewModel()) {
    val devoluciones by viewModel.devoluciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val estadoFiltro by viewModel.estadoFiltro.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showDrawer by viewModel.showDrawer.collectAsState()

    val isAdmin = UserSession.isAdmin()
    val usuarioId = UserSession.userId

    LaunchedEffect(Unit) { viewModel.cargarDatos() }

    // Diálogos
    var showAprobarDialog by remember { mutableStateOf(false) }
    var devolucionIdToAprobar by remember { mutableStateOf<Long?>(null) }
    var showRechazarDialog by remember { mutableStateOf(false) }
    var devolucionIdToRechazar by remember { mutableStateOf<Long?>(null) }
    var motivoRechazo by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dev. a Proveedores") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.abrirNuevaDevolucion() }) {
                Icon(Icons.Default.Add, "Nueva solicitud")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Buscador
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar solicitud, factura o proveedor...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Filtros de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("TODOS", "PENDIENTE", "APROBADA", "RECHAZADA").forEach { estado ->
                    FilterChip(
                        selected = estadoFiltro == estado,
                        onClick = { viewModel.setEstadoFiltro(estado) },
                        label = { Text(estado) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val paginatedList = viewModel.paginatedDevoluciones
                if (paginatedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron solicitudes.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(paginatedList) { dev ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Solicitud: ${dev.numeroDevolucion ?: "Pendiente"}", fontWeight = FontWeight.Bold)
                                    Text("Factura Lote: ${dev.numeroFacturaLote ?: ""}")
                                    Text("Proveedor: ${dev.proveedorNombre}")
                                    Text("Estado: ${dev.estado}", color = when (dev.estado) {
                                        "PENDIENTE" -> Color(0xFFFFA000)
                                        "APROBADA" -> Color(0xFF4CAF50)
                                        "RECHAZADA" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    })
                                    dev.detalles.forEach { det ->
                                        val med = viewModel.obtenerMedicamentoDesdeDetalle(det)
                                        Text("- ${med?.nombre ?: "Producto"} x${det.cantidadDevuelta}")
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (isAdmin && dev.estado == "PENDIENTE") {
                                            IconButton(onClick = {
                                                devolucionIdToAprobar = dev.id
                                                showAprobarDialog = true
                                            }) { Icon(Icons.Default.Check, "Aprobar", tint = Color(0xFF4CAF50)) }
                                            IconButton(onClick = {
                                                devolucionIdToRechazar = dev.id
                                                motivoRechazo = ""
                                                showRechazarDialog = true
                                            }) { Icon(Icons.Default.Close, "Rechazar", tint = Color(0xFFF44336)) }
                                        }
                                        // Acción de impresión (placeholder)
                                        IconButton(onClick = { /* Imprimir: lógica futura */ }) {
                                            Icon(Icons.Default.MoreVert, "Más opciones")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Paginación
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.setPage(currentPage - 1) },
                            enabled = currentPage > 1
                        ) { Text("← Anterior") }
                        Text("${currentPage} / ${viewModel.totalPages}")
                        TextButton(
                            onClick = { viewModel.setPage(currentPage + 1) },
                            enabled = currentPage < viewModel.totalPages
                        ) { Text("Siguiente →") }
                    }
                }
            }
        }
    }

    // ====================== DRAWER NUEVA DEVOLUCIÓN ======================
    if (showDrawer) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarDrawer() },
            sheetState = sheetState
        ) {
            DevolucionProveedorForm(viewModel = viewModel, usuarioId = usuarioId)
        }
    }

    // ====================== DIÁLOGO APROBAR ======================
    if (showAprobarDialog) {
        AlertDialog(
            onDismissRequest = { showAprobarDialog = false },
            title = { Text("¿Aprobar devolución?") },
            text = { Text("Confirma la aprobación de esta devolución.") },
            confirmButton = {
                TextButton(onClick = {
                    devolucionIdToAprobar?.let {
                        viewModel.aprobarDevolucion(it, usuarioId) { showAprobarDialog = false }
                    }
                }) { Text("Aprobar") }
            },
            dismissButton = { TextButton(onClick = { showAprobarDialog = false }) { Text("Cancelar") } }
        )
    }

    // ====================== DIÁLOGO RECHAZAR ======================
    if (showRechazarDialog) {
        AlertDialog(
            onDismissRequest = { showRechazarDialog = false },
            title = { Text("Motivo del rechazo") },
            text = {
                OutlinedTextField(
                    value = motivoRechazo,
                    onValueChange = { motivoRechazo = it },
                    label = { Text("Escriba el motivo...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    devolucionIdToRechazar?.let {
                        viewModel.rechazarDevolucion(it, usuarioId, motivoRechazo) { showRechazarDialog = false }
                    }
                }) { Text("Rechazar") }
            },
            dismissButton = { TextButton(onClick = { showRechazarDialog = false }) { Text("Cancelar") } }
        )
    }
}

// ====================== FORMULARIO DENTRO DEL BOTTOM SHEET ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionProveedorForm(viewModel: DevolucionesProveedorViewModel, usuarioId: Long) {
    val lotes by viewModel.lotes.collectAsState()
    val busquedaLote by viewModel.busquedaLote.collectAsState()
    val loteSeleccionado by viewModel.loteSeleccionado.collectAsState()
    val itemsDevolucion by viewModel.itemsDevolucion.collectAsState()
    val motivo by viewModel.motivo.collectAsState()
    val proveedores by viewModel.proveedores.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nueva Devolución a Proveedor", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (loteSeleccionado == null) {
            OutlinedTextField(
                value = busquedaLote,
                onValueChange = { viewModel.setBusquedaLote(it) },
                label = { Text("Buscar factura de lote...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(viewModel.lotesFiltrados) { lote ->
                    val proveedor = proveedores.find { it.id == lote.proveedorId }
                    ListItem(
                        headlineContent = { Text(lote.factura ?: "Sin factura") },
                        supportingContent = { Text(proveedor?.nombre ?: "") },
                        modifier = Modifier.clickable { viewModel.seleccionarLote(lote) }
                    )
                }
            }
        } else {
            // Lote seleccionado
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lote: ${loteSeleccionado!!.factura}", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        viewModel.seleccionarLote(loteSeleccionado!!) // resetea selección
                    }) { Text("Cambiar") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Cantidades a Devolver")
            itemsDevolucion.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.medicamentoNombre, fontWeight = FontWeight.Bold)
                        Text("Stock: ${item.cantidadDisponible}", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = item.cantidadDevuelta.toString(),
                        onValueChange = {
                            val cant = it.toIntOrNull() ?: 0
                            viewModel.actualizarCantidad(item.loteDetalleId, cant)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(70.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = motivo,
                onValueChange = { viewModel.setMotivo(it) },
                label = { Text("Motivo de la devolución") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.solicitarDevolucion(
                        usuarioId = usuarioId,
                        onSuccess = { viewModel.cerrarDrawer() },
                        onError = { /* mostrar error */ }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Solicitud")
            }
        }
    }
}