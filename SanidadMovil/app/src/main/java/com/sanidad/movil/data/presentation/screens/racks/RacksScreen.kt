package com.sanidad.movil.presentation.screens.racks

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
import com.sanidad.movil.data.repository.RackRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RacksScreen(
    rackRepository: RackRepository = remember { RackRepository() }
) {
    val viewModel: RacksViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RacksViewModel(rackRepository) as T
            }
        }
    )
    val racks by viewModel.racks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Racks") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(racks) { rack ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(rack.nombre, style = MaterialTheme.typography.titleLarge)
                            Text("Dim: ${rack.ancho}x${rack.alto}x${rack.profundidad} | Activo: ${if (rack.activo) "Sí" else "No"}")
                            if (rack.descripcion != null) Text("Descripción: ${rack.descripcion}")
                        }
                    }
                }
            }
        }
    }
}