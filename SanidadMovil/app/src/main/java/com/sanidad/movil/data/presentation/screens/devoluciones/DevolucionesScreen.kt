package com.sanidad.movil.presentation.screens.devoluciones

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
import com.sanidad.movil.data.repository.DevolucionRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionesScreen(
    devolucionRepository: DevolucionRepository = remember { DevolucionRepository() }
) {
    val viewModel: DevolucionesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DevolucionesViewModel(devolucionRepository) as T
            }
        }
    )
    val devoluciones by viewModel.devoluciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Devoluciones") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(devoluciones) { dev ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Dev. #${dev.numeroDevolucion} - Estado: ${dev.estado}", style = MaterialTheme.typography.titleLarge)
                            Text("Factura: ${dev.numeroFactura} | Solicitante: ${dev.usuarioSolicitanteNombre}")
                            Text("Fecha solicitud: ${dev.fechaSolicitud} | Aprobación: ${dev.fechaAprobacion ?: "N/A"}")
                            Text("Total devuelto: C$${dev.totalDevuelto} (efectivo: ${dev.montoDevueltoEfectivo}, saldo: ${dev.montoDevueltoSaldo})")
                            dev.detalles.forEach { d ->
                                Text("- ${d.medicamentoNombre} x${d.cantidadDevuelta} = ${d.subtotal}")
                            }
                        }
                    }
                }
            }
        }
    }
}