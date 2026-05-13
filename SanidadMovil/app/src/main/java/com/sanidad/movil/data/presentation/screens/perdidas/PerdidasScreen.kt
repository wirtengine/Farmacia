package com.sanidad.movil.presentation.screens.perdidas

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
import com.sanidad.movil.data.repository.PerdidasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerdidasScreen(
    perdidasRepository: PerdidasRepository = remember { PerdidasRepository() }
) {
    val viewModel: PerdidasViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PerdidasViewModel(perdidasRepository) as T
            }
        }
    )
    val vencidos by viewModel.vencidos.collectAsState()
    val inmoviles by viewModel.inmoviles.collectAsState()
    val inconsistencias by viewModel.inconsistencias.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Pérdidas") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                resumen?.let {
                    item { Text("Resumen:", style = MaterialTheme.typography.headlineMedium) }
                    item { Text("Vencidos: ${it.totalProductosVencidos}") }
                    item { Text("Inmóviles: ${it.totalProductosInmoviles}") }
                    item { Text("Inconsistencias: ${it.totalInconsistencias}") }
                    item { Text("Pérdida estimada: C$${it.perdidaEstimada}") }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }

                item { Text("Vencidos", style = MaterialTheme.typography.titleLarge) }
                items(vencidos) { v ->
                    Column {
                        Text("${v.nombre} (Lote ${v.lote}) vence ${v.fechaVencimiento} - stock: ${v.cantidad}")
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { Text("Inmóviles", style = MaterialTheme.typography.titleLarge) }
                items(inmoviles) { i ->
                    Text("${i.nombre}: ${i.cantidad} unidades, inmóvil ${i.diasInmovil} días")
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { Text("Inconsistencias", style = MaterialTheme.typography.titleLarge) }
                items(inconsistencias) { inc ->
                    Text("${inc.nombre}: sistema ${inc.stockSistema}, real ${inc.stockReal} (dif ${inc.diferencia})")
                }
            }
        }
    }
}