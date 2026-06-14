package com.sanidad.movil.presentation.screens.devolucionesProveedor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.*

private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Warning = Color(0xFFF59E0B)
private val Danger = Color(0xFFEF4444)
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionesProveedorScreen(viewModel: DevolucionesProveedorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()
    val usuarioId = UserSession.userId ?: 0L

    // Diálogo de aprobar (rechazo comentado porque no existe en backend)
    if (uiState.showAprobarDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.ocultarAprobarDialog() },
            title = { Text("¿Aprobar devolución?") },
            text = { Text("Confirma la aprobación de esta devolución.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarAprobar(usuarioId) }) { Text("Aprobar") }
            },
            dismissButton = { TextButton(onClick = { viewModel.ocultarAprobarDialog() }) { Text("Cancelar") } }
        )
    }
    // Si en el futuro agregas rechazo, descomenta este bloque y las variables de estado en el ViewModel
    /*
    if (uiState.showRechazarDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.ocultarRechazarDialog() },
            title = { Text("Motivo del rechazo") },
            text = {
                OutlinedTextField(
                    value = uiState.motivoRechazo,
                    onValueChange = { viewModel.setMotivoRechazo(it) },
                    label = { Text("Escriba el motivo...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarRechazar(usuarioId) }) { Text("Rechazar") }
            },
            dismissButton = { TextButton(onClick = { viewModel.ocultarRechazarDialog() }) { Text("Cancelar") } }
        )
    }
    */

    Scaffold(
        containerColor = Slate50,
        topBar = { TopAppBar(title = { Text("Devoluciones a Proveedores") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.abrirNuevaDevolucion() }) {
                Icon(Icons.Default.Add, "Nueva solicitud")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Buscador
            OutlinedTextField(
                value = uiState.searchTerm,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar solicitud, factura o proveedor...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Filtros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("TODOS", "PENDIENTE", "APROBADA", "RECHAZADA").forEach { estado ->
                    FilterChip(
                        selected = uiState.estadoFiltro == estado,
                        onClick = { viewModel.setEstadoFiltro(estado) },
                        label = { Text(estado) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Error
            uiState.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tabla
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column {
                    // Encabezados
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Slate50).padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TableHeader("N° Solicitud", Modifier.weight(1.5f))
                        TableHeader("Factura Lote", Modifier.weight(1.5f))
                        TableHeader("Proveedor", Modifier.weight(1.5f))
                        TableHeader("Productos", Modifier.weight(2.5f))
                        TableHeader("Estado", Modifier.weight(1f))
                        TableHeader("Acciones", Modifier.weight(2f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (uiState.isLoading && uiState.devoluciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (uiState.devoluciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron solicitudes.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(viewModel.paginatedDevoluciones, key = { it.id }) { dev ->
                                DevolucionProveedorRow(
                                    devolucion = dev,
                                    isAdmin = isAdmin,
                                    onAprobar = { viewModel.mostrarAprobarDialog(dev.id) },
                                    // onRechazar comentado porque no existe en backend
                                    onRechazar = {},
                                    onImprimir = { viewModel.imprimirPDF(dev) },
                                    onWhatsApp = { viewModel.enviarWhatsApp(dev) }
                                )
                            }
                        }
                    }

                    // Paginación
                    if (viewModel.totalPages > 1) {
                        Divider(color = Slate200)
                        PaginationBar(
                            currentPage = uiState.currentPage,
                            totalPages = viewModel.totalPages,
                            onPage = { viewModel.setPage(it) }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet nueva devolución
    if (uiState.showDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarDrawer() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            NuevaDevolucionProveedorSheet(
                lotes = viewModel.lotesFiltrados,
                busquedaLote = uiState.busquedaLote,
                onBusquedaChange = { viewModel.setBusquedaLote(it) },
                loteSeleccionado = uiState.loteSeleccionado,
                onLoteSelected = { viewModel.seleccionarLote(it) },
                itemsDevolucion = uiState.itemsDevolucion,
                onCantidadChange = { id, cant -> viewModel.actualizarCantidad(id, cant) },
                motivo = uiState.motivo,
                onMotivoChange = { viewModel.setMotivo(it) },
                proveedores = uiState.proveedores,
                onSolicitar = { viewModel.solicitarYEnviarWhatsApp(usuarioId) },
                onCancel = { viewModel.cerrarDrawer() },
                onChangeLote = { viewModel.abrirNuevaDevolucion() }
            )
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate500,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun DevolucionProveedorRow(
    devolucion: DevolucionProveedorResponse,
    isAdmin: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    onImprimir: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val estadoColor = when (devolucion.estado) {
        "PENDIENTE" -> Warning
        "APROBADA" -> Success
        "RECHAZADA" -> Danger
        else -> Slate500
    }
    val estadoBg = when (devolucion.estado) {
        "PENDIENTE" -> Color(0xFFFEF3C7)
        "APROBADA" -> Color(0xFFD1FAE5)
        "RECHAZADA" -> Color(0xFFFEE2E2)
        else -> Slate100
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(devolucion.numeroDevolucion, Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
            Text(devolucion.numeroFacturaLote ?: "N/A", Modifier.weight(1.5f), color = Slate700, fontSize = 14.sp)
            Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.1f)) {
                Text(devolucion.proveedorNombre, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Row(Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                devolucion.detalles.take(3).forEach { det ->
                    Surface(shape = RoundedCornerShape(8.dp), color = Slate100, border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(det.medicamentoNombre, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(" x${det.cantidadDevuelta}", fontSize = 12.sp, color = Primary)
                        }
                    }
                }
                if (devolucion.detalles.size > 3) Text("+${devolucion.detalles.size - 3}", color = Slate400, fontSize = 12.sp)
            }
            Surface(shape = RoundedCornerShape(40.dp), color = estadoBg, modifier = Modifier.weight(1f)) {
                Text(devolucion.estado, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = estadoColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Row(Modifier.weight(2f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin && devolucion.estado == "PENDIENTE") {
                    IconButton(onClick = onAprobar, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, null, tint = Success) }
                    // IconButton(onClick = onRechazar, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = Danger) }
                }
                IconButton(onClick = onImprimir, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Print, null, tint = Slate400) }
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Share, null, tint = Color(0xFF25D366)) }
            }
        }
        Divider(color = Slate100)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Slate50).padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onPage(currentPage - 1) }, enabled = currentPage > 1) { Text("← Anterior") }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            Surface(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) Primary else Color.Transparent)
                    .clickable { onPage(page) },
                color = if (isActive) Primary else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(page.toString(), color = if (isActive) White else Slate500, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        TextButton(onClick = { onPage(currentPage + 1) }, enabled = currentPage < totalPages) { Text("Siguiente →") }
    }
}

@Composable
private fun NuevaDevolucionProveedorSheet(
    lotes: List<LoteResponse>,
    busquedaLote: String,
    onBusquedaChange: (String) -> Unit,
    loteSeleccionado: LoteResponse?,
    onLoteSelected: (LoteResponse) -> Unit,
    itemsDevolucion: List<ItemDevolucionProv>,
    onCantidadChange: (Long, Int) -> Unit,
    motivo: String,
    onMotivoChange: (String) -> Unit,
    proveedores: List<ProveedorResponse>,
    onSolicitar: () -> Unit,
    onCancel: () -> Unit,
    onChangeLote: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Nueva Devolución a Proveedor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (loteSeleccionado == null) {
            OutlinedTextField(value = busquedaLote, onValueChange = onBusquedaChange, label = { Text("Buscar factura de lote...") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(200.dp)) {
                items(lotes, key = { it.id }) { lote ->
                    Surface(modifier = Modifier.fillMaxWidth().clickable { onLoteSelected(lote) },
                        shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(lote.factura ?: "Sin factura", fontWeight = FontWeight.Bold); Text(lote.proveedorNombre, color = Slate400) }
                            Icon(Icons.Default.ChevronRight, null, tint = Slate400)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Lote: ${loteSeleccionado.factura}", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onChangeLote) { Text("Cambiar") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Cantidades a Devolver", style = MaterialTheme.typography.labelMedium, color = Slate500)
            itemsDevolucion.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.medicamentoNombre, fontWeight = FontWeight.Bold)
                        Text("Stock: ${item.cantidadDisponible}", fontSize = 12.sp, color = Slate400)
                    }
                    OutlinedTextField(value = item.cantidadDevuelta.toString(),
                        onValueChange = { val cant = it.toIntOrNull() ?: 0; onCantidadChange(item.loteDetalleId, cant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(70.dp), shape = RoundedCornerShape(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = motivo, onValueChange = onMotivoChange, label = { Text("Motivo de la devolución") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(10.dp))
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) { Text("Cancelar") }
            Button(onClick = onSolicitar, modifier = Modifier.weight(1f), enabled = loteSeleccionado != null,
                shape = RoundedCornerShape(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                Text("🚀 Enviar Solicitud y PDF")
            }
        }
    }
}