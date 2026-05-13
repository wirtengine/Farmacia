package com.sanidad.movil.presentation.screens.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.repository.ClienteRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    clienteRepository: ClienteRepository = remember { ClienteRepository() }
) {
    val viewModel: ClientesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ClientesViewModel(clienteRepository) as T
            }
        }
    )
    val clientes by viewModel.clientes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Clientes") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(clientes) { cliente ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cliente.nombre, style = MaterialTheme.typography.titleLarge)
                            Text("Cédula: ${cliente.cedula} | Tel: ${cliente.telefono ?: "N/A"}")
                            Text("Email: ${cliente.email ?: "N/A"} | Saldo: C$${String.format("%.2f", cliente.saldo)}")
                            Text("Activo: ${if (cliente.activo) "Sí" else "No"}")
                        }
                    }
                }
            }
        }
    }
}