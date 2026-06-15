package com.sanidad.movil.presentation.screens.proveedores

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession

// Paleta fiel al diseño web
private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
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
fun ProveedoresScreen(viewModel: ProveedoresViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin = UserSession.isAdmin()

    // Diálogo de confirmación
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarDesactivar() },
            title = { Text("Dar de baja proveedor") },
            text = { Text("¿Desea dar de baja a este proveedor?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarDesactivar() }) {
                    Text("Dar de baja", color = Danger)
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
            ProveedoresHeader(
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
                        TableHeader("RUC", Modifier.weight(1.5f))
                        TableHeader("Razón Social / Nombre", Modifier.weight(2.5f))
                        TableHeader("Teléfono", Modifier.weight(1.5f))
                        TableHeader("Correo", Modifier.weight(2f))
                        if (isAdmin) TableHeader("Acciones", Modifier.weight(1.5f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (state.isLoading && state.proveedores.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.proveedores.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron proveedores activos.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(
                                items = viewModel.paginatedProveedores,
                                key = { _, p -> p.id }
                            ) { _, prov ->
                                ProveedorRow(
                                    proveedor = prov,
                                    isAdmin = isAdmin,
                                    onEdit = { viewModel.abrirEdicion(prov) },
                                    onDesactivar = { viewModel.solicitarDesactivar(prov.id) }
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

    // Bottom Sheet formulario
    if (state.showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarSheet() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ProveedorForm(
                formData = state.formData,
                isEditMode = state.isEditMode,
                formError = state.formError,
                onCampoChange = { campo, valor -> viewModel.updateCampo(campo, valor) },
                onGuardar = { viewModel.guardarProveedor() },
                onCancel = { viewModel.cerrarSheet() }
            )
        }
    }
}

// ---------- Componentes ----------

@Composable
private fun ProveedoresHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isAdmin: Boolean,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            "Directorio de Proveedores",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Gestión de entidades comerciales y suministros",
            fontSize = 14.sp,
            color = Slate500
        )
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
                placeholder = { Text("Buscar por nombre o RUC...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(40.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate200,
                    focusedBorderColor = Primary
                )
            )
            if (isAdmin) {
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("＋ Nuevo Proveedor")
                }
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
private fun ProveedorRow(
    proveedor: com.sanidad.movil.data.remote.dto.ProveedorResponse,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDesactivar: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RUC
            Text(text = proveedor.ruc, modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = Slate500)
            // Nombre
            Text(text = proveedor.nombre, modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
            // Teléfono
            Text(text = proveedor.telefono ?: "—", modifier = Modifier.weight(1.5f), color = Slate700, fontSize = 14.sp)
            // Correo
            Text(text = proveedor.email ?: "—", modifier = Modifier.weight(2f), color = Slate700, fontSize = 14.sp)
            // Acciones
            if (isAdmin) {
                Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = Primary) }
                    IconButton(onClick = onDesactivar, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Danger) }
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
        TextButton(onClick = { onPage(currentPage - 1) }, enabled = currentPage > 1) { Text("← Anterior") }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            // CORRECCIÓN: Surface con Modifier.clickable y sin onClick
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

@Composable
private fun ProveedorForm(
    formData: ProveedorFormState,
    isEditMode: Boolean,
    formError: String?,
    onCampoChange: (String, String) -> Unit,
    onGuardar: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Text(
            text = if (isEditMode) "Editar Proveedor" else "Nuevo Proveedor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        // Datos Fiscales
        Text("Datos Fiscales", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = formData.ruc,
            onValueChange = { onCampoChange("ruc", it) },
            label = { Text("RUC *") },
            enabled = !isEditMode,
            placeholder = { Text("Ej: J031000000") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = formData.nombre,
            onValueChange = { onCampoChange("nombre", it) },
            label = { Text("Nombre o Razón Social *") },
            placeholder = { Text("Nombre oficial") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Contacto
        Text("Contacto", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = formData.telefono,
            onValueChange = { onCampoChange("telefono", it) },
            label = { Text("Teléfono") },
            placeholder = { Text("+505 0000-0000") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = formData.email,
            onValueChange = { onCampoChange("email", it) },
            label = { Text("Correo Electrónico") },
            placeholder = { Text("ejemplo@proveedor.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        if (formError != null) {
            Spacer(Modifier.height(12.dp))
            Text(formError, color = Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) { Text("Cancelar") }
            Button(onClick = onGuardar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Success)) {
                Text(if (isEditMode) "Actualizar" else "Guardar")
            }
        }
    }
}