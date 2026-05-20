package com.sanidad.movil.data.presentation.screens.devoluciones

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
fun DevolucionesScreen(viewModel: DevolucionesViewModel = viewModel()) {
    val devoluciones by viewModel.devoluciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val estadoFiltro by viewModel.estadoFiltro.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showDrawer by viewModel.showDrawer.collectAsState()

    val isAdmin = UserSession.isAdmin()
    val usuarioId = UserSession.userId

    LaunchedEffect(Unit) { viewModel.cargarDatos() }

    // Diálogos de confirmación
    var showAprobarDialog by remember { mutableStateOf(false) }
    var devolucionAprobarId by remember { mutableStateOf<Long?>(null) }
    var aprobarAccion by remember { mutableStateOf(true) }
    var showMotivoRechazoDialog by remember { mutableStateOf(false) }
    var motivoRechazo by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Devoluciones") }) },
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
                placeholder = { Text("Buscar devolución o factura...") },
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
                        Text("No se encontraron devoluciones.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(paginatedList) { dev ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Dev. ${dev.numeroDevolucion ?: "---"}", fontWeight = FontWeight.Bold)
                                    Text("Factura: ${dev.numeroFactura} | Solicitante: ${dev.usuarioSolicitanteNombre}")
                                    Text("Estado: ${dev.estado}", color = when (dev.estado) {
                                        "PENDIENTE" -> Color(0xFFFFA000)
                                        "APROBADA" -> Color(0xFF4CAF50)
                                        "RECHAZADA" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    })
                                    Text("Total reembolso: C$${String.format("%.2f", dev.totalDevuelto)}")
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
                                                devolucionAprobarId = dev.id
                                                aprobarAccion = true
                                                showAprobarDialog = true
                                            }) { Icon(Icons.Default.Check, "Aprobar", tint = Color(0xFF4CAF50)) }
                                            IconButton(onClick = {
                                                devolucionAprobarId = dev.id
                                                aprobarAccion = false
                                                showMotivoRechazoDialog = true
                                            }) { Icon(Icons.Default.Close, "Rechazar", tint = Color(0xFFF44336)) }
                                        }
                                        IconButton(onClick = { /* Imprimir: mostrar diálogo simple */ }) {
                                            Icon(Icons.Default.MoreVert, "Imprimir")
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
            DevolucionForm(viewModel = viewModel, usuarioId = usuarioId)
        }
    }

    // ====================== DIÁLOGO APROBAR ======================
    if (showAprobarDialog) {
        AlertDialog(
            onDismissRequest = { showAprobarDialog = false },
            title = { Text(if (aprobarAccion) "¿Aprobar devolución?" else "¿Rechazar devolución?") },
            text = { Text(if (aprobarAccion) "Los productos volverán al inventario." else "Se rechazará la solicitud.") },
            confirmButton = {
                TextButton(onClick = {
                    devolucionAprobarId?.let {
                        viewModel.aprobarDevolucion(it, aprobarAccion, usuarioId, null) {
                            showAprobarDialog = false
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showAprobarDialog = false }) { Text("Cancelar") } }
        )
    }

    // ====================== DIÁLOGO MOTIVO RECHAZO ======================
    if (showMotivoRechazoDialog) {
        AlertDialog(
            onDismissRequest = { showMotivoRechazoDialog = false },
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
                    devolucionAprobarId?.let {
                        viewModel.aprobarDevolucion(it, false, usuarioId, motivoRechazo) {
                            showMotivoRechazoDialog = false
                            motivoRechazo = ""
                        }
                    }
                }) { Text("Rechazar") }
            },
            dismissButton = { TextButton(onClick = { showMotivoRechazoDialog = false }) { Text("Cancelar") } }
        )
    }
}

// ====================== FORMULARIO DENTRO DEL BOTTOM SHEET ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionForm(viewModel: DevolucionesViewModel, usuarioId: Long) {
    val ventas by viewModel.ventas.collectAsState()
    val busquedaVenta by viewModel.busquedaVenta.collectAsState()
    val ventaSeleccionada by viewModel.ventaSeleccionada.collectAsState()
    val itemsDevolucion by viewModel.itemsDevolucion.collectAsState()
    val motivo by viewModel.motivo.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nueva Devolución", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (ventaSeleccionada == null) {
            // Buscador de ventas
            OutlinedTextField(
                value = busquedaVenta,
                onValueChange = { viewModel.setBusquedaVenta(it) },
                label = { Text("Buscar factura...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(viewModel.ventasFiltradas) { venta ->
                    ListItem(
                        headlineContent = { Text(venta.numeroFactura) },
                        supportingContent = { Text(venta.fecha) },
                        trailingContent = { Text("C$${String.format("%.2f", venta.total)}") },
                        modifier = Modifier.clickable { viewModel.seleccionarVenta(venta) }
                    )
                }
            }
        } else {
            // Venta seleccionada
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Factura: ${ventaSeleccionada!!.numeroFactura}", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        viewModel.seleccionarVenta(ventaSeleccionada!!) // resetea selección
                    }) { Text("Cambiar") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Productos disponibles para retorno")
            itemsDevolucion.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.producto, fontWeight = FontWeight.Bold)
                        Text("Original: ${item.cantidadMax} unidades", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = item.cantidadDevuelta.toString(),
                        onValueChange = {
                            val cant = it.toIntOrNull() ?: 0
                            viewModel.actualizarCantidad(item.ventaDetalleId, cant)
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
                label = { Text("Motivo de la solicitud") },
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
                Text("Crear Solicitud")
            }
        }
    }
}