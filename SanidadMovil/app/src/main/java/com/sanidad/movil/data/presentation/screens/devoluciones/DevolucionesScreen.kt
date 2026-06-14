package com.sanidad.movil.presentation.screens.devoluciones

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.*
import java.text.NumberFormat
import java.util.Locale

// Paleta de colores
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
fun DevolucionesScreen(viewModel: DevolucionesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()
    val usuarioId = UserSession.userId

    // Diálogos
    if (state.showAprobarDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.ocultarAprobarDialog() },
            title = { Text(if (state.aprobarAccion) "¿Aprobar devolución?" else "¿Rechazar devolución?") },
            text = { Text(if (state.aprobarAccion) "Los productos volverán al inventario." else "Se rechazará la solicitud.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarAprobar(usuarioId) }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { viewModel.ocultarAprobarDialog() }) { Text("Cancelar") } }
        )
    }
    if (state.showMotivoRechazoDialog) {
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.ocultarMotivoRechazoDialog() },
            title = { Text("Motivo del rechazo") },
            text = {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it; viewModel.setMotivoRechazo(it) },
                    label = { Text("Escriba el motivo...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarAprobar(usuarioId) }) { Text("Rechazar") }
            },
            dismissButton = { TextButton(onClick = { viewModel.ocultarMotivoRechazoDialog() }) { Text("Cancelar") } }
        )
    }

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            DevolucionesHeader(
                searchTerm = state.searchTerm,
                onSearchChange = { viewModel.setSearch(it) },
                estadoFiltro = state.estadoFiltro,
                onEstadoChange = { viewModel.setEstadoFiltro(it) },
                onAdd = { viewModel.abrirNuevaDevolucion() }
            )

            // Mensaje de error
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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

            // Tabla
            Card(
                modifier = Modifier.weight(1f).padding(16.dp),
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
                        TableHeader("N° Devolución", Modifier.weight(1.5f))
                        TableHeader("Factura", Modifier.weight(1.2f))
                        TableHeader("Solicitante", Modifier.weight(1.5f))
                        TableHeader("Productos", Modifier.weight(2.5f))
                        TableHeader("Estado", Modifier.weight(1f))
                        TableHeader("Reembolso", Modifier.weight(1f))
                        TableHeader("Acciones", Modifier.weight(1.5f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (state.isLoading && state.devoluciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.devoluciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron devoluciones.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(
                                items = viewModel.paginatedDevoluciones,
                                key = { _, d -> d.id }
                            ) { _, dev ->
                                DevolucionRow(
                                    devolucion = dev,
                                    isAdmin = isAdmin,
                                    onAprobar = { viewModel.mostrarAprobarDialog(dev.id, true) },
                                    onRechazar = { viewModel.mostrarMotivoRechazoDialog(dev.id) },
                                    onImprimir = { /* Implementar ticket */ },
                                    obtenerMedicamento = { viewModel.obtenerMedicamentoDesdeDetalle(it) }
                                )
                            }
                        }
                    }

                    // Paginación
                    if (state.totalPages > 1) {
                        Divider(color = Slate200)
                        PaginationBar(
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            onPage = { viewModel.setPage(it) }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet para nueva devolución
    if (state.showDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarDrawer() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            NuevaDevolucionSheet(
                ventas = viewModel.ventasFiltradas,
                busquedaVenta = state.busquedaVenta,
                onBusquedaChange = { viewModel.setBusquedaVenta(it) },
                ventaSeleccionada = state.ventaSeleccionada,
                onVentaSelected = { viewModel.seleccionarVenta(it) },
                itemsDevolucion = state.itemsDevolucion,
                onCantidadChange = { id, cant -> viewModel.actualizarCantidad(id, cant) },
                motivo = state.motivo,
                onMotivoChange = { viewModel.setMotivo(it) },
                onSolicitar = { viewModel.solicitarDevolucion(usuarioId) },
                onCancel = { viewModel.cerrarDrawer() },
                onChangeVenta = { viewModel.abrirNuevaDevolucion() }
            )
        }
    }
}

// ------- Componentes -------

@Composable
private fun DevolucionesHeader(
    searchTerm: String,
    onSearchChange: (String) -> Unit,
    estadoFiltro: String,
    onEstadoChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "Devoluciones",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar devolución o factura...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(40.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Slate200, focusedBorderColor = Primary)
            )
            // Grupo de filtros
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.background(Slate200, RoundedCornerShape(40.dp)).padding(4.dp)
            ) {
                listOf("TODOS", "PENDIENTE", "APROBADA", "RECHAZADA").forEach { estado ->
                    val isActive = estadoFiltro == estado
                    TextButton(
                        onClick = { onEstadoChange(estado) },
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isActive) White else Color.Transparent,
                            contentColor = if (isActive) Primary else Slate500
                        ),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(estado.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("＋ Nueva Solicitud")
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    Text(
        text = text.uppercase(),  // ← corregido
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate500,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun DevolucionRow(
    devolucion: DevolucionResponse,
    isAdmin: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    onImprimir: () -> Unit,
    obtenerMedicamento: (DevolucionDetalleResponse) -> MedicamentoResponse?
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
            // N° Devolución
            Text(
                text = devolucion.numeroDevolucion ?: "---",
                modifier = Modifier.weight(1.5f),
                fontWeight = FontWeight.Bold,
                color = Slate900,
                fontSize = 14.sp
            )
            // Factura
            Text(
                text = devolucion.numeroFactura,
                modifier = Modifier.weight(1.2f),
                color = Slate700,
                fontSize = 14.sp
            )
            // Solicitante
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    devolucion.usuarioSolicitanteNombre,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            // Productos
            Row(
                modifier = Modifier.weight(2.5f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                devolucion.detalles.take(3).forEach { det ->
                    val med = obtenerMedicamento(det)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            if (med?.imagen != null) {
                                AsyncImage(
                                    model = "http://172.16.66.6:8080/${med.imagen.replace("\\", "/")}",
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = med?.nombre ?: "S/N",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "x${det.cantidadDevuelta}",
                                fontSize = 12.sp,
                                color = Primary
                            )
                        }
                    }
                }
                if (devolucion.detalles.size > 3) {
                    Text("+${devolucion.detalles.size - 3}", color = Slate400, fontSize = 12.sp)
                }
            }
            // Estado
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = estadoBg,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    devolucion.estado,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = estadoColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            // Reembolso
            Text(
                text = formatCurrency(devolucion.totalDevuelto ?: 0.0),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = Primary,
                fontSize = 14.sp
            )
            // Acciones
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAdmin && devolucion.estado == "PENDIENTE") {
                    IconButton(onClick = onAprobar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, null, tint = Success)
                    }
                    IconButton(onClick = onRechazar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = Danger)
                    }
                }
                IconButton(onClick = onImprimir, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, null, tint = Slate400)
                }
            }
        }
        Divider(color = Slate100)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, onPage: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Slate50).padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onPage(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Text("← Anterior")
        }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            // Corregido: usar Surface sin onClick, envolver en una que acepte click
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) Primary else Color.Transparent)
                    .clickable { onPage(page) },
                color = if (isActive) Primary else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = page.toString(),
                        color = if (isActive) White else Slate500,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        TextButton(
            onClick = { onPage(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Text("Siguiente →")
        }
    }
}

@Composable
private fun NuevaDevolucionSheet(
    ventas: List<VentaResponse>,
    busquedaVenta: String,
    onBusquedaChange: (String) -> Unit,
    ventaSeleccionada: VentaResponse?,
    onVentaSelected: (VentaResponse) -> Unit,
    itemsDevolucion: List<ItemDevolucion>,
    onCantidadChange: (Long, Int) -> Unit,
    motivo: String,
    onMotivoChange: (String) -> Unit,
    onSolicitar: () -> Unit,
    onCancel: () -> Unit,
    onChangeVenta: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Nueva Devolución", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (ventaSeleccionada == null) {
            OutlinedTextField(
                value = busquedaVenta,
                onValueChange = onBusquedaChange,
                label = { Text("Buscar factura...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.height(8.dp))
            // Se reemplaza LazyColumn por Column con limitación de altura y scroll
            Column(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ventas.forEach { venta ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onVentaSelected(venta) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(venta.numeroFactura, fontWeight = FontWeight.Bold)
                                Text(venta.fecha, color = Slate400)
                            }
                            Text(formatCurrency(venta.total), fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            // Venta seleccionada
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Factura: ${ventaSeleccionada.numeroFactura}", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onChangeVenta) { Text("Cambiar") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Productos disponibles para retorno", style = MaterialTheme.typography.labelMedium, color = Slate500)
            itemsDevolucion.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.producto, fontWeight = FontWeight.Bold)
                        Text("Original: ${item.cantidadMax} unidades", fontSize = 12.sp, color = Slate400)
                    }
                    OutlinedTextField(
                        value = item.cantidadDevuelta.toString(),
                        onValueChange = { val cant = it.toIntOrNull() ?: 0; onCantidadChange(item.ventaDetalleId, cant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(70.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = motivo,
                onValueChange = onMotivoChange,
                label = { Text("Motivo de la solicitud") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) {
                Text("Cancelar")
            }
            Button(
                onClick = onSolicitar,
                modifier = Modifier.weight(1f),
                enabled = ventaSeleccionada != null,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                Text("Crear Solicitud")
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)