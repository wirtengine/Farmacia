package com.sanidad.movil.presentation.screens.usuarios

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Paleta fiel al CSS
private val Primary = Color(0xFF4F46E5)
private val PrimaryHover = Color(0xFF4338CA)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
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
private val Info = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            UsuariosHeader(
                searchTerm = state.searchTerm,
                onSearchChange = { viewModel.setSearch(it) },
                onAdd = { viewModel.abrirNuevo() }
            )

            // Error
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column {
                    // Encabezados de tabla
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate50)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TableHeader("Usuario", Modifier.weight(2f))
                        TableHeader("Rol / Permisos", Modifier.weight(2f))
                        TableHeader("Estado", Modifier.weight(1.5f))
                        TableHeader("Acciones", Modifier.weight(1f), alignEnd = true)
                    }
                    Divider(color = Slate200)

                    if (state.isLoading && state.usuarios.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (state.usuarios.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron usuarios.", color = Slate400)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(
                                items = viewModel.paginatedUsuarios,
                                key = { _, u -> u.id }
                            ) { _, usuario ->
                                UsuarioRow(
                                    usuario = usuario,
                                    onEdit = { viewModel.abrirEdicion(usuario) }
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
            UsuarioForm(
                formData = state.formData,
                isEditMode = state.isEditMode,
                formError = state.formError,
                onCampoChange = { campo, valor -> viewModel.updateCampo(campo, valor) },
                onGuardar = { viewModel.guardarUsuario() },
                onCancel = { viewModel.cerrarSheet() }
            )
        }
    }
}

// ---------- Componentes ----------

@Composable
private fun UsuariosHeader(
    searchTerm: String,
    onSearchChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            "Gestión de Personal",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Administra los accesos y roles de la farmacia",
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
                value = searchTerm,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar por usuario o rol...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(40.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate200,
                    focusedBorderColor = Primary
                )
            )
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("＋ Nuevo Usuario")
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    Text(
        text = text.uppercase(),   // CORREGIDO: usar uppercase en lugar de textTransform
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate500,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun UsuarioRow(
    usuario: com.sanidad.movil.data.remote.dto.UsuarioResponse,
    onEdit: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con inicial
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Success),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.username.firstOrNull()?.uppercase() ?: "U",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            // Nombre de usuario
            Text(
                text = usuario.username,
                modifier = Modifier.weight(2f),
                fontWeight = FontWeight.Bold,
                color = Slate900,
                fontSize = 14.sp
            )
            // Rol
            val rolColor = when (usuario.rol) {
                "ADMIN" -> Color(0xFF0369A1)
                "VENDEDOR" -> Color(0xFF92400E)
                "FARMACEUTICO" -> Color(0xFF9A3412)
                else -> Slate500
            }
            val rolBg = when (usuario.rol) {
                "ADMIN" -> Color(0xFFE0F2FE)
                "VENDEDOR" -> Color(0xFFFEF3C7)
                "FARMACEUTICO" -> Color(0xFFFED7AA)
                else -> Slate100
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = rolBg,
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = usuario.rol,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = rolColor
                )
            }
            // Estado (siempre Activo)
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Success)
                )
                Spacer(Modifier.width(6.dp))
                Text("Activo", color = Success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            // Acción editar
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Primary, modifier = Modifier.size(20.dp))
                }
            }
        }
        Divider(color = Slate100)
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, onPage: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(16.dp),
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
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) Info else Color.Transparent)
                    .clickable { onPage(page) },   // CORREGIDO: clickable en lugar de onClick en Surface
                color = if (isActive) Info else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        page.toString(),
                        color = if (isActive) White else Slate500,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        TextButton(onClick = { onPage(currentPage + 1) }, enabled = currentPage < totalPages) {
            Text("Siguiente →")
        }
    }
}

@Composable
private fun UsuarioForm(
    formData: UsuarioFormState,
    isEditMode: Boolean,
    formError: String?,
    onCampoChange: (String, String) -> Unit,
    onGuardar: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = if (isEditMode) "Editar Perfil" else "Nuevo Integrante",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        // Username
        OutlinedTextField(
            value = formData.username,
            onValueChange = { onCampoChange("username", it) },
            label = { Text("Nombre de Usuario") },
            enabled = !isEditMode,
            placeholder = { Text("Ej. mgarcia") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Slate500,
                disabledBorderColor = Slate200,
                disabledContainerColor = Slate50
            )
        )
        Spacer(Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = formData.password,
            onValueChange = { onCampoChange("password", it) },
            label = { Text(if (isEditMode) "Nueva Contraseña (opcional)" else "Contraseña de Acceso") },
            placeholder = { Text(if (isEditMode) "Dejar vacío para mantener" else "Mínimo 6 caracteres") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Rol
        Text(
            "Rol Asignado",
            fontWeight = FontWeight.Bold,
            color = Slate600,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("VENDEDOR", "FARMACEUTICO", "ADMIN").forEach { role ->
                val isSelected = formData.rol == role
                Button(
                    onClick = { onCampoChange("rol", role) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Primary else White,
                        contentColor = if (isSelected) White else Slate600
                    ),
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
                ) {
                    Text(role, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Error del formulario
        if (formError != null) {
            Spacer(Modifier.height(12.dp))
            Text(formError, color = Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(40.dp)
            ) { Text("Cancelar") }
            Button(
                onClick = onGuardar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                Text(if (isEditMode) "Actualizar" else "Guardar")
            }
        }
    }
}