package com.sanidad.movil.presentation.screens.recetas

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
import com.sanidad.movil.data.repository.RecetaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetasScreen(
    recetaRepository: RecetaRepository = remember { RecetaRepository() }
) {
    val viewModel: RecetasViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RecetasViewModel(recetaRepository) as T
            }
        }
    )
    val recetas by viewModel.recetas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Recetas") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(recetas) { r ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Código: ${r.codigoMinsa ?: "N/A"} | Estado: ${r.estado}")
                            Text("Farmacéutico: ${r.farmaceuticoUsername ?: ""} (ID: ${r.farmaceuticoId})")
                            Text("Fecha subida: ${r.fechaSubida ?: "N/A"} | Venta: ${r.ventaId ?: "Sin venta"}")
                        }
                    }
                }
            }
        }
    }
}