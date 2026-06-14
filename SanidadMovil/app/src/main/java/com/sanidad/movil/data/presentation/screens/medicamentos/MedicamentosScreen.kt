package com.sanidad.movil.presentation.screens.medicamentos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import java.text.NumberFormat
import java.util.Locale

private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
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
fun MedicamentosScreen(viewModel: MedicamentosViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()

    // Diálogo de confirmación
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarConfirmacion() },
            title = { Text(state.confirmTitle) },
            text = { Text(state.confirmText) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarAccion() }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarConfirmacion() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            MedicamentosHeader(
                searchQuery = state.searchQuery,
                onSearchChange = { viewModel.setSearch(it) },
                isAdmin = isAdmin,
                onAdd = { viewModel.abrirNuevo() },
                onPDF = { viewModel.generarPDF() }
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
                        TableHeader("Imagen", Modifier.weight(0.8f))
                        TableHeader("Medicamento", Modifier.weight(2f))
                        TableHeader("Registro", Modifier.weight(1.5f))
                        TableHeader("Fabricante", Modifier.weight(1.5f))
                        TableHeader("Presentación", Modifier.weight(1f))
                        TableHeader("Vía", Modifier.weight(0.8f))
                        TableHeader("Precio", Modifier.weight(1f))
                        TableHeader("Estado", Modifier.weight(0.8f))
                        if (isAdmin) TableHeader("Acciones", Modifier.weight(1.2f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (state.isLoading && state.medicamentos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.medicamentos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron medicamentos.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(
                                items = viewModel.paginatedMedicamentos,
                                key = { _, m -> m.id }
                            ) { _, med ->
                                MedicamentoRow(
                                    medicamento = med,
                                    isAdmin = isAdmin,
                                    onEdit = { viewModel.abrirEdicion(med) },
                                    onDesactivar = { viewModel.solicitarDesactivar(med.id) },
                                    onReactivar = { viewModel.solicitarReactivar(med.id) }
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

    // Bottom Sheet formulario
    if (state.showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarSheet() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            MedicamentoForm(
                formData = state.formData,
                isEditMode = state.isEditMode,
                formError = state.formError,
                onCampoChange = { campo, valor -> viewModel.actualizarCampo(campo, valor) },
                onImageSelected = { viewModel.setImageUri(it) },
                onGuardar = { viewModel.guardarMedicamento() },
                onCancel = { viewModel.cerrarSheet() }
            )
        }
    }
}

@Composable
private fun MedicamentosHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isAdmin: Boolean,
    onAdd: () -> Unit,
    onPDF: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Catálogo Farmacéutico", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar medicamento...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(40.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Slate200, focusedBorderColor = Primary)
            )
            OutlinedButton(
                onClick = onPDF,
                shape = RoundedCornerShape(40.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("PDF")
            }
            if (isAdmin) {
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("＋ Nuevo") }
            }
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
private fun MedicamentoRow(
    medicamento: com.sanidad.movil.data.remote.dto.MedicamentoResponse,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDesactivar: () -> Unit,
    onReactivar: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen
            Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterStart) {
                if (medicamento.imagen != null) {
                    AsyncImage(
                        model = "http://172.16.66.6:8080/${medicamento.imagen.replace("\\", "/")}",
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("—", color = Slate400)
                }
            }
            // Nombre
            Text(medicamento.nombre, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
            // Registro
            Text(medicamento.registroSanitario, modifier = Modifier.weight(1.5f), color = Slate500, fontSize = 13.sp)
            // Fabricante
            Text(medicamento.fabricante ?: "—", modifier = Modifier.weight(1.5f), color = Slate700, fontSize = 13.sp)
            // Presentación
            Surface(shape = RoundedCornerShape(40.dp), color = Slate100) {
                Text(medicamento.presentacion ?: "Tableta", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            }
            // Vía
            Surface(shape = RoundedCornerShape(40.dp), color = Primary.copy(alpha = 0.1f)) {
                Text(medicamento.via ?: "ORAL", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
            // Precio
            Text(formatCurrency(medicamento.precioUnitario), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
            // Estado
            val (statusText, statusBg, statusColor) = if (medicamento.activo) {
                Triple("Activo", Color(0xFFD1FAE5), Color(0xFF065F46))
            } else {
                Triple("Inactivo", Color(0xFFFEE2E2), Color(0xFF991B1B))
            }
            Surface(shape = RoundedCornerShape(40.dp), color = statusBg, modifier = Modifier.weight(0.8f)) {
                Text(statusText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
            // Acciones
            if (isAdmin) {
                Row(modifier = Modifier.weight(1.2f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = Primary) }
                    if (medicamento.activo) {
                        IconButton(onClick = onDesactivar, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Danger) }
                    } else {
                        IconButton(onClick = onReactivar, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = Success) }
                    }
                }
            } else {
                Spacer(Modifier.weight(1.2f))
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
        TextButton(onClick = { onPage(currentPage - 1) }, enabled = currentPage > 1) { Text("← Anterior") }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
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
        TextButton(onClick = { onPage(currentPage + 1) }, enabled = currentPage < totalPages) { Text("Siguiente →") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicamentoForm(
    formData: MedicamentoFormState,
    isEditMode: Boolean,
    formError: String?,
    onCampoChange: (String, Any) -> Unit,
    onImageSelected: (android.net.Uri?) -> Unit,
    onGuardar: () -> Unit,
    onCancel: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImageSelected(uri)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(if (isEditMode) "Editar Medicamento" else "Nuevo Medicamento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.nombre,
            onValueChange = { onCampoChange("nombre", it) },
            label = { Text("Nombre Comercial *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.registroSanitario,
                onValueChange = { onCampoChange("registroSanitario", it) },
                label = { Text("Reg. Sanitario *") },
                enabled = !isEditMode,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = formData.fabricante,
                onValueChange = { onCampoChange("fabricante", it) },
                label = { Text("Fabricante *") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.precioUnitario,
                onValueChange = { onCampoChange("precioUnitario", it) },
                label = { Text("Precio Unitario *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = formData.receta, onCheckedChange = { onCampoChange("receta", it) })
            Text("Requiere receta médica")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = { launcher.launch("image/*") }, shape = RoundedCornerShape(10.dp)) {
            Text("Seleccionar imagen")
        }

        if (formError != null) {
            Spacer(Modifier.height(8.dp))
            Text(formError, color = Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) { Text("Cancelar") }
            Button(onClick = onGuardar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                Text("Guardar")
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "NI"))
    return format.format(value)
}