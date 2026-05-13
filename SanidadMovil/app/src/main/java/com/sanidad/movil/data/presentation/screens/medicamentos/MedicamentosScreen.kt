package com.sanidad.movil.presentation.screens.medicamentos

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
import com.sanidad.movil.data.repository.MedicamentoRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentosScreen(
    medicamentoRepository: MedicamentoRepository = remember { MedicamentoRepository() }
) {
    val viewModel: MedicamentosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MedicamentosViewModel(medicamentoRepository) as T
            }
        }
    )
    val medicamentos by viewModel.medicamentos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Medicamentos") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(medicamentos) { med ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(med.nombre, style = MaterialTheme.typography.titleLarge)
                            Text("Presentación: ${med.presentacion} | Precio: C$${med.precioUnitario}")
                            Text("Vía: ${med.via} | Registro: ${med.registroSanitario}")
                            Text("Fabricante: ${med.fabricante} | Tipo venta: ${med.tipoVenta}")
                            Text("Receta: ${if (med.receta) "Sí" else "No"} | Activo: ${if (med.activo) "Sí" else "No"}")
                        }
                    }
                }
            }
        }
    }
}