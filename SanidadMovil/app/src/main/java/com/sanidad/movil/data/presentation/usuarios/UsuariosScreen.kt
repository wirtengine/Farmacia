package com.sanidad.movil.data.presentation.screens.usuarios

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel = viewModel()) {
    val usuarios by viewModel.usuarios.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarUsuarios() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Usuarios") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.abrirNuevo() }) {
                Icon(Icons.Default.Add, "Nuevo usuario")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Buscador
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por usuario o rol...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val paginatedList = viewModel.paginatedUsuarios
                if (paginatedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron usuarios.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(paginatedList) { user ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar con inicial
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(0xFF1976D2)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                user.username.firstOrNull()?.uppercase() ?: "U",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.username, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Rol: ", fontSize = 12.sp)
                                            Text(
                                                user.rol,
                                                color = when (user.rol) {
                                                    "ADMIN" -> Color(0xFF1565C0)
                                                    "VENDEDOR" -> Color(0xFF2E7D32)
                                                    "FARMACEUTICO" -> Color(0xFFEF6C00)
                                                    else -> Color.Gray
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.abrirEdicion(user) }) {
                                        Icon(Icons.Default.Edit, "Editar")
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

    // Bottom Sheet para formulario
    if (showSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.cerrarSheet() },
            sheetState = sheetState
        ) {
            UsuarioForm(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioForm(viewModel: UsuariosViewModel) {
    val form by viewModel.formData.collectAsState()
    val isEdit by viewModel.isEditMode.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            if (isEdit) "Editar Perfil" else "Nuevo Integrante",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Username
        OutlinedTextField(
            value = form.username,
            onValueChange = { viewModel.updateCampo("username", it) },
            label = { Text("Nombre de Usuario") },
            enabled = !isEdit,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password
        OutlinedTextField(
            value = form.password,
            onValueChange = { viewModel.updateCampo("password", it) },
            label = {
                Text(if (isEdit) "Nueva Contraseña (opcional)" else "Contraseña de Acceso")
            },
            placeholder = { Text(if (isEdit) "Dejar vacío para mantener" else "Mínimo 6 caracteres") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Rol
        Text("Rol Asignado", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("VENDEDOR", "FARMACEUTICO", "ADMIN").forEach { role ->
                FilterChip(
                    selected = form.rol == role,
                    onClick = { viewModel.updateCampo("rol", role) },
                    label = { Text(role) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { viewModel.cerrarSheet() }) { Text("Cancelar") }
            Button(onClick = {
                viewModel.guardarUsuario(
                    onSuccess = { viewModel.cerrarSheet() },
                    onError = { /* mostrar error */ }
                )
            }) { Text(if (isEdit) "Actualizar" else "Guardar") }
        }
    }
}