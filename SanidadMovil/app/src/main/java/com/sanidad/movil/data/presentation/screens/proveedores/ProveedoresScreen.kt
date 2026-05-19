package com.sanidad.movil.data.presentation.screens.proveedores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(
    viewModel: ProveedoresViewModel = viewModel()
) {
    val proveedores by viewModel.proveedores.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()

    val isAdmin = UserSession.isAdmin()

    LaunchedEffect(Unit) {
        viewModel.cargarProveedores()
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var proveedorToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Proveedores") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { viewModel.abrirNuevo() }) {
                    Icon(Icons.Default.Add, "Nuevo proveedor")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o RUC...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val paginatedList = viewModel.paginatedProveedores
                if (paginatedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron proveedores activos.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(paginatedList) { prov ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prov.nombre, fontWeight = FontWeight.Bold)
                                            Text("RUC: ${prov.ruc}", style = MaterialTheme.typography.bodySmall)
                                            if (prov.telefono != null) Text("Tel: ${prov.telefono}", style = MaterialTheme.typography.bodySmall)
                                            if (prov.email != null) Text(prov.email, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (isAdmin) {
                                            Row {
                                                IconButton(onClick = { viewModel.abrirEdicion(prov) }) {
                                                    Icon(Icons.Default.Edit, "Editar")
                                                }
                                                IconButton(onClick = {
                                                    proveedorToDelete = prov.id
                                                    showConfirmDialog = true
                                                }) {
                                                    Icon(Icons.Default.Delete, "Desactivar")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Paginación
                if (viewModel.totalPages > 1) {
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
            ProveedorForm(
                viewModel = viewModel,
                onDismiss = { viewModel.cerrarSheet() }
            )
        }
    }

    // Diálogo de confirmación para desactivar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Dar de baja proveedor") },
            text = { Text("¿Desea dar de baja a este proveedor?") },
            confirmButton = {
                TextButton(onClick = {
                    proveedorToDelete?.let { viewModel.desactivarProveedor(it) { showConfirmDialog = false } }
                }) { Text("Dar de baja") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedorForm(
    viewModel: ProveedoresViewModel,
    onDismiss: () -> Unit
) {
    val form by viewModel.formData.collectAsState()
    val isEdit by viewModel.isEditMode.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            if (isEdit) "Editar Proveedor" else "Nuevo Proveedor",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = form.ruc,
            onValueChange = { viewModel.updateCampo("ruc", it) },
            label = { Text("RUC *") },
            enabled = !isEdit,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.nombre,
            onValueChange = { viewModel.updateCampo("nombre", it) },
            label = { Text("Nombre o Razón Social *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.telefono,
            onValueChange = { viewModel.updateCampo("telefono", it) },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.email,
            onValueChange = { viewModel.updateCampo("email", it) },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(onClick = {
                viewModel.guardarProveedor(
                    onSuccess = onDismiss,
                    onError = { /* mostrar error */ }
                )
            }) { Text("Guardar") }
        }
    }
}