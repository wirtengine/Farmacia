package com.sanidad.movil.data.presentation.screens.lotes

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.LoteResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotesScreen(
    viewModel: LotesViewModel = viewModel()
) {
    val lotes by viewModel.lotes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filtroStock by viewModel.filtroStock.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()

    val isAdmin = UserSession.isAdmin()

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var loteToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lotes") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { viewModel.abrirNuevo() }) {
                    Icon(Icons.Default.Add, "Nuevo lote")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Filtros de stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroStock == "todos",
                    onClick = { viewModel.setFiltro("todos") },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtroStock == "stock",
                    onClick = { viewModel.setFiltro("stock") },
                    label = { Text("En Stock") }
                )
                FilterChip(
                    selected = filtroStock == "agotado",
                    onClick = { viewModel.setFiltro("agotado") },
                    label = { Text("Agotados") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar factura, proveedor o medicamento...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val paginatedList = viewModel.paginatedLotes
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(paginatedList) { lote ->
                        LoteCard(
                            lote = lote,
                            isAdmin = isAdmin,
                            onDesactivar = {
                                loteToDelete = lote.id
                                showConfirmDialog = true
                            },
                            onImprimir = {
                                // Toast o Snackbar (omito implementación completa)
                            }
                        )
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

    // Bottom Sheet para formulario
    if (showSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarSheet() },
            sheetState = sheetState
        ) {
            LoteForm(
                viewModel = viewModel,
                onDismiss = { viewModel.cerrarSheet() }
            )
        }
    }

    // Diálogo de confirmación
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Desactivar lote") },
            text = { Text("¿Está seguro de desactivar este lote?") },
            confirmButton = {
                TextButton(onClick = {
                    loteToDelete?.let { viewModel.desactivarLote(it) { showConfirmDialog = false } }
                }) { Text("Desactivar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun LoteCard(
    lote: LoteResponse,
    isAdmin: Boolean,
    onDesactivar: () -> Unit,
    onImprimir: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(lote.factura ?: "Sin factura", fontWeight = FontWeight.Bold)
                val totalStock = lote.detalles?.sumOf { it.cantidad } ?: 0
                val vencido = lote.fechaVencimiento?.let {
                    try {
                        LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(LocalDate.now())
                    } catch (e: Exception) { false }
                } ?: false
                Text(
                    if (totalStock > 0) "En Stock ($totalStock)" else "Agotado",
                    color = when {
                        vencido -> Color.Red
                        totalStock > 0 -> Color(0xFF4CAF50)
                        else -> Color.Gray
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Proveedor: ${lote.proveedorNombre ?: ""}")
            Text("Fabricación: ${lote.fechaFabricacion ?: "N/A"} | Vencimiento: ${lote.fechaVencimiento ?: "N/A"}")
            lote.detalles?.forEach { detalle ->
                Text("- ${detalle.medicamentoNombre} x${detalle.cantidad}")
            }
            if (isAdmin && lote.activo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDesactivar) {
                        Icon(Icons.Default.Delete, "Desactivar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoteForm(
    viewModel: LotesViewModel,
    onDismiss: () -> Unit
) {
    val form by viewModel.formData.collectAsState()
    val proveedores by viewModel.proveedores.collectAsState()
    val medicamentos by viewModel.medicamentos.collectAsState()
    val racks by viewModel.racks.collectAsState()

    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nuevo Lote", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        // Factura (autogenerada, solo lectura)
        OutlinedTextField(
            value = form.factura,
            onValueChange = {},
            readOnly = true,
            label = { Text("Factura") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Proveedor
        var expandedProv by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedProv,
            onExpandedChange = { expandedProv = !expandedProv }
        ) {
            OutlinedTextField(
                value = proveedores.find { it.id == form.proveedorId }?.nombre ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Proveedor *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProv) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expandedProv, onDismissRequest = { expandedProv = false }) {
                proveedores.forEach { prov ->
                    DropdownMenuItem(
                        text = { Text(prov.nombre) },
                        onClick = {
                            viewModel.updateCampo("proveedorId", prov.id)
                            expandedProv = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Fechas (simplificado con campos de texto + date picker)
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        OutlinedTextField(
            value = form.fechaFabricacion,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fabricación") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    // Usar DatePickerDialog
                    val datePicker = DatePickerDialog(context)
                    datePicker.setOnDateSetListener { _, year, month, dayOfMonth ->
                        val fecha = LocalDate.of(year, month + 1, dayOfMonth).format(dateFormatter)
                        viewModel.updateCampo("fechaFabricacion", fecha)
                    }
                    datePicker.show()
                }) { Icon(Icons.Default.DateRange, null) }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.fechaVencimiento,
            onValueChange = {},
            readOnly = true,
            label = { Text("Vencimiento *") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    val datePicker = DatePickerDialog(context)
                    datePicker.setOnDateSetListener { _, year, month, dayOfMonth ->
                        val fecha = LocalDate.of(year, month + 1, dayOfMonth).format(dateFormatter)
                        viewModel.updateCampo("fechaVencimiento", fecha)
                    }
                    datePicker.show()
                }) { Icon(Icons.Default.DateRange, null) }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Productos", style = MaterialTheme.typography.titleMedium)
        form.detalles.forEachIndexed { index, detalle ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown medicamento
                var expandedMed by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMed,
                    onExpandedChange = { expandedMed = !expandedMed },
                    modifier = Modifier.weight(2f)
                ) {
                    OutlinedTextField(
                        value = medicamentos.find { it.id == detalle.medicamentoId }?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Medicamento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMed) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedMed, onDismissRequest = { expandedMed = false }) {
                        medicamentos.forEach { med ->
                            DropdownMenuItem(
                                text = { Text(med.nombre) },
                                onClick = {
                                    viewModel.updateDetalle(index, detalle.copy(medicamentoId = med.id))
                                    expandedMed = false
                                }
                            )
                        }
                    }
                }

                // Cantidad
                OutlinedTextField(
                    value = detalle.cantidad.toString(),
                    onValueChange = {
                        val cant = it.toIntOrNull() ?: 1
                        viewModel.updateDetalle(index, detalle.copy(cantidad = cant))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                // Botón eliminar
                IconButton(onClick = { viewModel.removeDetalle(index) }) {
                    Icon(Icons.Default.Delete, "Quitar")
                }
            }

            // Selector de ubicación simplificado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showUbicacionDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showUbicacionDialog = true }) {
                    Text(
                        if (detalle.rackId != null) "📍 Rack: ${racks.find { it.id == detalle.rackId }?.nombre ?: ""} N${detalle.nivel + 1} C${detalle.columna + 1}"
                        else "📌 Asignar ubicación"
                    )
                }
                if (showUbicacionDialog) {
                    AlertDialog(
                        onDismissRequest = { showUbicacionDialog = false },
                        title = { Text("Seleccionar rack") },
                        text = {
                            Column {
                                racks.forEach { rack ->
                                    TextButton(onClick = {
                                        viewModel.updateDetalle(index, detalle.copy(rackId = rack.id))
                                        // Podrías pedir nivel, columna, profundidad aquí
                                    }) { Text(rack.nombre) }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nivel: ${detalle.nivel}")
                                Slider(value = detalle.nivel.toFloat(), onValueChange = { viewModel.updateDetalle(index, detalle.copy(nivel = it.toInt())) }, valueRange = 0f..5f)
                                Text("Columna: ${detalle.columna}")
                                Slider(value = detalle.columna.toFloat(), onValueChange = { viewModel.updateDetalle(index, detalle.copy(columna = it.toInt())) }, valueRange = 0f..5f)
                                Text("Profundidad: ${detalle.profundidadIndex}")
                                Slider(value = detalle.profundidadIndex.toFloat(), onValueChange = { viewModel.updateDetalle(index, detalle.copy(profundidadIndex = it.toInt())) }, valueRange = 0f..5f)
                            }
                        },
                        confirmButton = { TextButton(onClick = { showUbicacionDialog = false }) { Text("Listo") } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Botón añadir detalle
        TextButton(onClick = { viewModel.addDetalle() }) {
            Text("+ Añadir Medicamento")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botones guardar/cancelar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(onClick = {
                viewModel.guardarLote(
                    onSuccess = onDismiss,
                    onError = { /* mostrar error */ }
                )
            }) { Text("Guardar Entrada") }
        }
    }
}