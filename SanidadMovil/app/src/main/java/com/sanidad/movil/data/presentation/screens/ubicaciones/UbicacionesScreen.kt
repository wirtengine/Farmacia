package com.sanidad.movil.presentation.screens.ubicaciones

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
import com.sanidad.movil.data.repository.UbicacionRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbicacionesScreen(
    ubicacionRepository: UbicacionRepository = remember { UbicacionRepository() }
) {
    val viewModel: UbicacionesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return UbicacionesViewModel(ubicacionRepository) as T
            }
        }
    )
    val ubicaciones by viewModel.ubicaciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Ubicaciones") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(ubicaciones) { u ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(u.medicamentoNombre, style = MaterialTheme.typography.titleLarge)
                            Text("Rack: ${u.rackNombre} | Pos: [${u.nivel},${u.columna},${u.profundidadIndex}]")
                            Text("Cantidad: ${u.cantidad} | Activo: ${if (u.activo) "Sí" else "No"}")
                        }
                    }
                }
            }
        }
    }
}