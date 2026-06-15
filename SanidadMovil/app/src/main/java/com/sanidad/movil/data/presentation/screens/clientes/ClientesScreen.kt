package com.sanidad.movil.presentation.screens.clientes

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession
import java.text.NumberFormat
import java.util.Locale

// Paleta fiel al diseño web
private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(viewModel: ClientesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val tienePermiso = UserSession.rol in listOf("ADMIN", "VENDEDOR")

    if (state.showConfirmDeactivate) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarDesactivar() },
            title = { Text("Desactivar cliente") },
            text = { Text("¿Está seguro de desactivar este cliente?") },
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
            ClientesHeader(
                searchQuery = state.searchTerm,
                onSearchChange = { viewModel.setSearch(it) },
                onAdd = { viewModel.abrirNuevo() },
                onPDF = { /* Imprimir reporte general */ }
            )

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
                            Text(error, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.limpiarError() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = Danger)
                            }
                        }
                    }
                }
            }

            // Contenedor principal adaptativo (sin BoxWithConstraints)
            val configuration = LocalConfiguration.current
            val wideMode = configuration.screenWidthDp > 600

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    if (state.isLoading && state.clientes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.clientes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron clientes.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (wideMode) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate50)
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        TableHeader("Cédula", Modifier.weight(1.5f))
                                        TableHeader("Nombre", Modifier.weight(2f))
                                        TableHeader("Teléfono", Modifier.weight(1.5f))
                                        TableHeader("Saldo", Modifier.weight(1f))
                                        TableHeader("Estado", Modifier.weight(1f))
                                        TableHeader("Acciones", Modifier.weight(1.5f), alignEnd = true)
                                    }
                                    Divider(color = Slate200)
                                }
                            }

                            itemsIndexed(
                                items = viewModel.paginatedClientes,
                                key = { _, c -> c.id }
                            ) { index, cliente ->
                                if (wideMode) {
                                    ClienteRow(
                                        cliente = cliente,
                                        tienePermiso = tienePermiso,
                                        onEdit = { viewModel.abrirEdicion(cliente) },
                                        onDeactivate = { viewModel.solicitarDesactivar(cliente.id) },
                                        onPrint = { /* Imprimir ficha */ },
                                        isLast = index == viewModel.paginatedClientes.lastIndex
                                    )
                                } else {
                                    ClienteCardCompact(
                                        cliente = cliente,
                                        tienePermiso = tienePermiso,
                                        onEdit = { viewModel.abrirEdicion(cliente) },
                                        onDeactivate = { viewModel.solicitarDesactivar(cliente.id) },
                                        onPrint = { /* Imprimir ficha */ }
                                    )
                                }
                            }

                            if (state.totalPages > 1) {
                                item {
                                    Divider(color = Slate200)
                                    PaginationBar(
                                        currentPage = state.currentPage,
                                        totalPages = state.totalPages,
                                        onPageSelected = { viewModel.setPage(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showForm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarFormulario() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ClienteForm(
                formData = state.formData,
                isEditMode = state.isEditMode,
                formError = state.formError,
                isLoading = state.isLoading,
                onFieldChange = { campo, valor -> viewModel.actualizarCampo(campo, valor) },
                onSave = { viewModel.guardarCliente() },
                onCancel = { viewModel.cerrarFormulario() }
            )
        }
    }
}

/* ──────────────────── Header (con botón Nuevo siempre visible) ──────────────────── */
@Composable
private fun ClientesHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAdd: () -> Unit,
    onPDF: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Gestión de Clientes", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
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
                placeholder = { Text("Buscar por cédula o nombre...", fontSize = 14.sp, color = Slate400) },
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
                Text("PDF", fontSize = 14.sp)
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) { Text("＋ Nuevo", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
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

/* ──────────────────── Modo Tabla (ancho) ──────────────────── */
@Composable
private fun ClienteRow(
    cliente: com.sanidad.movil.data.remote.dto.ClienteResponse,
    tienePermiso: Boolean,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onPrint: () -> Unit,
    isLast: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cliente.cedula, Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
            Text(cliente.nombre, Modifier.weight(2f), color = Slate700, fontSize = 14.sp)
            Text(cliente.telefono ?: "—", Modifier.weight(1.5f), color = Slate500, fontSize = 14.sp)
            Text(formatCurrency(cliente.saldo), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 14.sp, textAlign = TextAlign.End)
            // Estado
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                val bg = if (cliente.activo) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                val fg = if (cliente.activo) Color(0xFF065F46) else Color(0xFF991B1B)
                Surface(shape = RoundedCornerShape(40.dp), color = bg) {
                    Text(
                        if (cliente.activo) "Activo" else "Inactivo",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg
                    )
                }
            }
            // Acciones
            Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, null, tint = Slate500)
                }
                if (tienePermiso && cliente.activo) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Primary)
                    }
                    IconButton(onClick = onDeactivate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Danger)
                    }
                }
            }
        }
        if (!isLast) Divider(color = Slate100, thickness = 1.dp)
    }
}

/* ──────────────────── Tarjeta Compacta mejorada ──────────────────── */
@Composable
private fun ClienteCardCompact(
    cliente: com.sanidad.movil.data.remote.dto.ClienteResponse,
    tienePermiso: Boolean,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onPrint: () -> Unit
) {
    val bg = if (cliente.activo) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val fg = if (cliente.activo) Color(0xFF065F46) else Color(0xFF991B1B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            // Badge de estado en esquina superior derecha
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(20.dp),
                color = bg
            ) {
                Text(
                    if (cliente.activo) "Activo" else "Inactivo",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg
                )
            }

            Column {
                // Fila 1: Nombre y cédula
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(cliente.nombre, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(cliente.cedula, fontSize = 13.sp, color = Slate500)
                }

                Spacer(Modifier.height(10.dp))

                // Fila 2: Teléfono, Saldo y Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InfoLabel("Teléfono")
                        Text(cliente.telefono ?: "—", fontSize = 13.sp, color = Slate700, textAlign = TextAlign.Center)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InfoLabel("Saldo")
                        Text(formatCurrency(cliente.saldo), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary, textAlign = TextAlign.Center)
                    }
                    // Acciones
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Print, null, tint = Slate500)
                        }
                        if (tienePermiso && cliente.activo) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, null, tint = Primary)
                            }
                            IconButton(onClick = onDeactivate, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLabel(text: String) {
    Text(text, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
}

/* ──────────────────── Paginación ──────────────────── */
@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Slate50).padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onPageSelected(currentPage - 1) }, enabled = currentPage > 1) {
            Text("← Anterior", color = if (currentPage > 1) Primary else Slate400)
        }
        val start = maxOf(1, currentPage - 2)
        val end = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) Primary else Color.Transparent)
                    .clickable { onPageSelected(page) },
                color = if (isActive) Primary else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(page.toString(), color = if (isActive) White else Slate600, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        TextButton(onClick = { onPageSelected(currentPage + 1) }, enabled = currentPage < totalPages) {
            Text("Siguiente →", color = if (currentPage < totalPages) Primary else Slate400)
        }
    }
}

/* ──────────────────── Formulario (sin cambios) ──────────────────── */
@Composable
private fun ClienteForm(
    formData: ClienteFormState,
    isEditMode: Boolean,
    formError: String?,
    isLoading: Boolean,
    onFieldChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditMode) "Actualizar Cliente" else "Registro de Cliente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Datos Personales", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = formData.cedula,
            onValueChange = { onFieldChange("cedula", it) },
            label = { Text("Cédula / Identificación *") },
            enabled = !isEditMode,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = formData.nombre,
            onValueChange = { onFieldChange("nombre", it) },
            label = { Text("Nombre Completo *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Contacto y Cuenta", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formData.telefono,
                onValueChange = { onFieldChange("telefono", it) },
                label = { Text("Teléfono") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = formData.saldo,
                onValueChange = { onFieldChange("saldo", it) },
                label = { Text("Saldo Inicial (C$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = formData.email,
            onValueChange = { onFieldChange("email", it) },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        if (formError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(formError, color = Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(40.dp)) {
                Text("Cancelar")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                } else {
                    Text(if (isEditMode) "Guardar Cambios" else "Registrar Cliente")
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "NI"))
    return format.format(value)
}