package com.sanidad.movil.presentation.screens.proveedores

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
import com.sanidad.movil.data.repository.ProveedorRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(
    proveedorRepository: ProveedorRepository = remember { ProveedorRepository() }
) {
    val viewModel: ProveedoresViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProveedoresViewModel(proveedorRepository) as T
            }
        }
    )
    val proveedores by viewModel.proveedores.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Proveedores") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(proveedores) { prov ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(prov.nombre, style = MaterialTheme.typography.titleLarge)
                            Text("RUC: ${prov.ruc} | Tel: ${prov.telefono ?: "N/A"} | Email: ${prov.email ?: "N/A"}")
                            Text("Activo: ${if (prov.activo) "Sí" else "No"}")
                        }
                    }
                }
            }
        }
    }
}