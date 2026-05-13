package com.sanidad.movil.presentation.screens.devolucionesProveedor

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
import com.sanidad.movil.data.repository.DevolucionProveedorRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionesProveedorScreen(
    devolucionProveedorRepository: DevolucionProveedorRepository = remember { DevolucionProveedorRepository() }
) {
    val viewModel: DevolucionesProveedorViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DevolucionesProveedorViewModel(devolucionProveedorRepository) as T
            }
        }
    )
    val devoluciones by viewModel.devoluciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Dev. Proveedor") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(devoluciones) { d ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Dev. #${d.numeroDevolucion} - ${d.estado}", style = MaterialTheme.typography.titleLarge)
                            Text("Proveedor: ${d.proveedorNombre} | Lote: ${d.loteId}")
                            Text("Solicitante: ${d.solicitadoPorNombre} | Fecha: ${d.fechaSolicitud}")
                            Text("Aprobado por: ${d.aprobadoPorNombre ?: "N/A"} | ${d.fechaAprobacion ?: ""}")
                            d.detalles.forEach { det ->
                                Text("- ${det.medicamentoNombre} x${det.cantidadDevuelta}")
                            }
                        }
                    }
                }
            }
        }
    }
}