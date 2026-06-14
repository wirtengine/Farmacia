package com.sanidad.movil.presentation.screens.lotes

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

// Paleta del diseño
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
fun LotesScreen(viewModel: LotesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()

    // Diálogo confirmación desactivar
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarDesactivar() },
            title = { Text("Desactivar lote") },
            text = { Text("¿Está seguro de desactivar este lote?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarDesactivar() }) {
                    Text("Desactivar", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarDesactivar() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            LotesHeader(
                filtroStock = state.filtroStock,
                onFiltroChange = { viewModel.setFiltroStock(it) },
                searchQuery = state.searchQuery,
                onSearchChange = { viewModel.setSearch(it) },
                isAdmin = isAdmin,
                onAdd = { viewModel.abrirNuevo() }
            )

            // Error flotante
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

            // Tabla de lotes
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column {
                    // Encabezados
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate50)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TableHeader("Imagen", Modifier.weight(0.8f))
                        TableHeader("Factura", Modifier.weight(1.2f))
                        TableHeader("Proveedor", Modifier.weight(1.5f))
                        TableHeader("Medicamentos", Modifier.weight(2.5f))
                        TableHeader("Vencimiento", Modifier.weight(1.5f))
                        TableHeader("Estado", Modifier.weight(1f))
                        if (isAdmin) TableHeader("Acciones", Modifier.weight(1f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (state.isLoading && state.lotes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.lotes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron lotes.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(
                                items = viewModel.paginatedLotes,
                                key = { _, l -> l.id }
                            ) { _, lote ->
                                LoteRow(
                                    lote = lote,
                                    medicamentos = state.medicamentos,
                                    proveedores = state.proveedores,
                                    isAdmin = isAdmin,
                                    onImprimir = { viewModel.imprimirLote(lote) },
                                    onDesactivar = { viewModel.solicitarDesactivar(lote.id) }
                                )
                            }
                        }
                    }

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

    // Bottom Sheet formulario nuevo lote
    if (state.showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarSheet() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            LoteForm(
                formData = state.formData,
                medicamentos = state.medicamentos,
                proveedores = state.proveedores,
                racks = state.racks,
                formError = state.formError,
                onCampoChange = { campo, valor -> viewModel.updateCampo(campo, valor) },
                onAddDetalle = { viewModel.addDetalle() },
                onRemoveDetalle = { viewModel.removeDetalle(it) },
                onDetalleChange = { index, detalle -> viewModel.updateDetalle(index, detalle) },
                onGuardar = { viewModel.guardarLote() },
                onCancel = { viewModel.cerrarSheet() }
            )
        }
    }
}

// ---------- Componentes ----------

@Composable
private fun LotesHeader(
    filtroStock: String,
    onFiltroChange: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isAdmin: Boolean,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Inventario de Lotes", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filtros de stock (píldoras)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.background(Slate100, RoundedCornerShape(40.dp)).padding(4.dp)
            ) {
                listOf("todos" to "Todos", "stock" to "En Stock", "agotado" to "Agotados").forEach { (key, label) ->
                    val isActive = filtroStock == key
                    TextButton(
                        onClick = { onFiltroChange(key) },
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isActive) White else Color.Transparent,
                            contentColor = if (isActive) Primary else Slate500
                        ),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) { Text(label, fontWeight = FontWeight.SemiBold) }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar factura, proveedor o medicamento...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(40.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Slate200, focusedBorderColor = Primary)
            )
            if (isAdmin) {
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("＋ Registrar Entrada") }
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    // CORRECCIÓN: usar .uppercase() en lugar de textTransform
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
private fun LoteRow(
    lote: LoteResponse,
    medicamentos: List<MedicamentoResponse>,
    proveedores: List<ProveedorResponse>,
    isAdmin: Boolean,
    onImprimir: () -> Unit,
    onDesactivar: () -> Unit
) {
    val totalStock = lote.detalles?.sumOf { it.cantidad } ?: 0
    val vencido = try {
        LocalDate.parse(lote.fechaVencimiento, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(LocalDate.now())
    } catch (e: Exception) { false }
    val proveedorNombre = proveedores.find { it.id == lote.proveedorId }?.nombre ?: "—"
    val primerMed = lote.detalles?.firstOrNull()?.let { medicamentos.find { m -> m.id == it.medicamentoId } }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del primer medicamento
            Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterStart) {
                if (primerMed?.imagen != null) {
                    AsyncImage(
                        model = "http://172.16.66.6:8080/${primerMed.imagen.replace("\\", "/")}",
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("—", color = Slate400)
                }
            }
            // Factura
            Text(lote.factura ?: "Sin factura", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
            // Proveedor
            Text(proveedorNombre, modifier = Modifier.weight(1.5f), color = Slate700, fontSize = 14.sp)
            // Chips de medicamentos
            Row(modifier = Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                lote.detalles?.take(3)?.forEach { det ->
                    val med = medicamentos.find { it.id == det.medicamentoId }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
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
                            Text("x${det.cantidad}", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if ((lote.detalles?.size ?: 0) > 3) Text("+${lote.detalles.size - 3}", color = Slate400, fontSize = 12.sp)
            }
            // Vencimiento
            Column(modifier = Modifier.weight(1.5f)) {
                Text(lote.fechaVencimiento ?: "—", fontSize = 13.sp, color = Slate700)
                if (vencido) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Danger
                    ) {
                        Text(
                            "Vencido",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Estado
            val statusText = if (lote.activo) (if (totalStock > 0) "En Stock" else "Agotado") else "Inactivo"
            val statusBg = when {
                !lote.activo -> Color(0xFFFEE2E2)
                totalStock > 0 -> Color(0xFFD1FAE5)
                else -> Color(0xFFFEF3C7)
            }
            val statusColor = when {
                !lote.activo -> Color(0xFF991B1B)
                totalStock > 0 -> Color(0xFF065F46)
                else -> Color(0xFF9A3412)
            }
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = statusBg,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            // Acciones
            if (isAdmin) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onImprimir, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Print, null, tint = Slate400)
                    }
                    if (lote.activo) {
                        IconButton(onClick = onDesactivar, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Danger)
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
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
        TextButton(onClick = { onPage(currentPage - 1) }, enabled = currentPage > 1) {
            Text("← Anterior")
        }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            // CORRECCIÓN: evitar Modifier.then y onClick en Surface
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
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
        TextButton(onClick = { onPage(currentPage + 1) }, enabled = currentPage < totalPages) {
            Text("Siguiente →")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoteForm(
    formData: LoteFormState,
    medicamentos: List<MedicamentoResponse>,
    proveedores: List<ProveedorResponse>,
    racks: List<RackResponse>,
    formError: String?,
    onCampoChange: (String, Any) -> Unit,
    onAddDetalle: () -> Unit,
    onRemoveDetalle: (Int) -> Unit,
    onDetalleChange: (Int, DetalleFormState) -> Unit,
    onGuardar: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Nuevo Lote", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.factura,
            onValueChange = {},
            readOnly = true,
            label = { Text("Factura") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Proveedor dropdown
        var expandedProv by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expandedProv, onExpandedChange = { expandedProv = !expandedProv }) {
            OutlinedTextField(
                value = proveedores.find { it.id == formData.proveedorId }?.nombre ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Proveedor *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProv) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(expanded = expandedProv, onDismissRequest = { expandedProv = false }) {
                proveedores.forEach { prov ->
                    DropdownMenuItem(
                        text = { Text(prov.nombre) },
                        onClick = {
                            onCampoChange("proveedorId", prov.id)
                            expandedProv = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.fechaFabricacion,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fabricación") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            val fecha = LocalDate.of(y, m + 1, d).format(DateTimeFormatter.ISO_LOCAL_DATE)
                            onCampoChange("fechaFabricacion", fecha)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )
            OutlinedTextField(
                value = formData.fechaVencimiento,
                onValueChange = {},
                readOnly = true,
                label = { Text("Vencimiento *") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            val fecha = LocalDate.of(y, m + 1, d).format(DateTimeFormatter.ISO_LOCAL_DATE)
                            onCampoChange("fechaVencimiento", fecha)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Productos", style = MaterialTheme.typography.titleMedium)
        formData.detalles.forEachIndexed { index, det ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Dropdown medicamento
                    var expandedMed by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedMed,
                        onExpandedChange = { expandedMed = !expandedMed },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = medicamentos.find { it.id == det.medicamentoId }?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Medicamento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMed) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedMed, onDismissRequest = { expandedMed = false }) {
                            medicamentos.forEach { med ->
                                DropdownMenuItem(
                                    text = { Text(med.nombre) },
                                    onClick = {
                                        onDetalleChange(index, det.copy(medicamentoId = med.id))
                                        expandedMed = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = det.cantidad.toString(),
                        onValueChange = { val cant = it.toIntOrNull() ?: 1; onDetalleChange(index, det.copy(cantidad = cant)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    IconButton(onClick = { onRemoveDetalle(index) }) {
                        Icon(Icons.Default.Delete, "Quitar", tint = Danger)
                    }
                }
                // Selector de ubicación simplificado (botón)
                TextButton(onClick = {
                    // Mostrar pequeño diálogo para seleccionar rack, nivel, columna
                }) {
                    Text(
                        if (det.rackId != null) "📍 Rack: ${racks.find { it.id == det.rackId }?.nombre ?: ""} N${det.nivel + 1} C${det.columna + 1}"
                        else "📌 Asignar ubicación"
                    )
                }
            }
        }
        TextButton(onClick = onAddDetalle) {
            Text("+ Añadir Medicamento")
        }

        if (formError != null) {
            Spacer(Modifier.height(8.dp))
            Text(formError, color = Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) {
                Text("Cancelar")
            }
            Button(onClick = onGuardar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                Text("Guardar Entrada")
            }
        }
    }
}