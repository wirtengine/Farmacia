package com.sanidad.movil.data.presentation.screens.clientes

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.UserSession
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(viewModel: ClientesViewModel = viewModel()) {
    val clientes by viewModel.clientes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()

    val tienePermiso = UserSession.rol in listOf("ADMIN", "VENDEDOR")

    LaunchedEffect(Unit) { viewModel.cargarClientes() }

    // Diálogo de confirmación para desactivar
    var showConfirmDialog by remember { mutableStateOf(false) }
    var clienteToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clientes") }) },
        floatingActionButton = {
            if (tienePermiso) {
                FloatingActionButton(onClick = { viewModel.abrirNuevo() }) {
                    Icon(Icons.Default.Add, "Nuevo cliente")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Buscador
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por cédula o nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val paginatedList = viewModel.paginatedClientes
                if (paginatedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron clientes.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(paginatedList) { cliente ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cliente.nombre, fontWeight = FontWeight.Bold)
                                        Text("Cédula: ${cliente.cedula}", fontSize = 12.sp)
                                        if (!cliente.telefono.isNullOrBlank()) Text("Tel: ${cliente.telefono}", fontSize = 12.sp)
                                        if (!cliente.email.isNullOrBlank()) Text(cliente.email, fontSize = 12.sp)
                                        Text("Saldo: ${formatCurrency(cliente.saldo)}", fontWeight = FontWeight.Bold)
                                        Text(
                                            if (cliente.activo) "Activo" else "Inactivo",
                                            color = if (cliente.activo) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                    // Acciones
                                    Column {
                                        IconButton(onClick = { /* Imprimir ficha: mostrar Snackbar */ }) {
                                            Icon(Icons.Default.Info, "Imprimir")
                                        }
                                        if (tienePermiso && cliente.activo) {
                                            IconButton(onClick = { viewModel.abrirEdicion(cliente) }) {
                                                Icon(Icons.Default.Edit, "Editar")
                                            }
                                            IconButton(onClick = {
                                                clienteToDelete = cliente.id
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
            ClienteForm(viewModel = viewModel)
        }
    }

    // Diálogo de confirmación para desactivar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Desactivar cliente") },
            text = { Text("¿Está seguro de desactivar este cliente?") },
            confirmButton = {
                TextButton(onClick = {
                    clienteToDelete?.let { viewModel.desactivarCliente(it) { showConfirmDialog = false } }
                }) { Text("Desactivar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteForm(viewModel: ClientesViewModel) {
    val form by viewModel.formData.collectAsState()
    val isEdit by viewModel.isEditMode.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            if (isEdit) "Actualizar Cliente" else "Registro de Cliente",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Cédula
        OutlinedTextField(
            value = form.cedula,
            onValueChange = { viewModel.updateCampo("cedula", it) },
            label = { Text("Cédula / Identificación *") },
            enabled = !isEdit,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Nombre
        OutlinedTextField(
            value = form.nombre,
            onValueChange = { viewModel.updateCampo("nombre", it) },
            label = { Text("Nombre Completo *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Teléfono
        OutlinedTextField(
            value = form.telefono,
            onValueChange = { viewModel.updateCampo("telefono", it) },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Email
        OutlinedTextField(
            value = form.email,
            onValueChange = { viewModel.updateCampo("email", it) },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Saldo
        OutlinedTextField(
            value = form.saldo,
            onValueChange = { viewModel.updateCampo("saldo", it) },
            label = { Text("Saldo Inicial (C$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { viewModel.cerrarSheet() }) { Text("Cancelar") }
            Button(onClick = {
                viewModel.guardarCliente(
                    onSuccess = { viewModel.cerrarSheet() },
                    onError = { /* mostrar error */ }
                )
            }) { Text(if (isEdit) "Guardar Cambios" else "Registrar Cliente") }
        }
    }
}

fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "NI"))
    return format.format(value)
}