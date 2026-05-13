package com.sanidad.movil.presentation.screens.lotes

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
import com.sanidad.movil.data.repository.LoteRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotesScreen(
    loteRepository: LoteRepository = remember { LoteRepository() }
) {
    val viewModel: LotesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return LotesViewModel(loteRepository) as T
            }
        }
    )
    val lotes by viewModel.lotes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Lotes") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(lotes) { lote ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Lote: ${lote.numeroLote}", style = MaterialTheme.typography.titleLarge)
                            Text("Proveedor: ${lote.proveedorNombre} (${lote.proveedorRuc})")
                            Text("Fabricación: ${lote.fechaFabricacion ?: "N/A"} | Vencimiento: ${lote.fechaVencimiento ?: "N/A"}")
                            Text("Factura: ${lote.factura ?: "N/A"} | Activo: ${if (lote.activo) "Sí" else "No"}")
                            lote.detalles.forEach { d ->
                                Text("- ${d.medicamentoNombre} (${d.medicamentoPresentacion}) x${d.cantidad} | Precio: ${d.precioUnitario}")
                            }
                        }
                    }
                }
            }
        }
    }
}