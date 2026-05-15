package com.sanidad.movil.data.presentation.screens.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.repository.VentaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    ventaRepository: VentaRepository = remember { VentaRepository() },
    onNuevaVenta: () -> Unit = {}
) {
    val viewModel: VentasViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return VentasViewModel(ventaRepository) as T
            }
        }
    )
    val ventas by viewModel.ventas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ventas") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaVenta,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva venta",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(ventas) { venta ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Factura: ${venta.numeroFactura}", style = MaterialTheme.typography.titleLarge)
                            Text("Fecha: ${venta.fecha} | Tipo: ${venta.tipo}")
                            Text("Cliente: ${venta.clienteNombre ?: "Sin cliente"} | Vendedor: ${venta.usuarioUsername}")
                            Text("Subtotal: ${venta.subtotal} | IVA: ${venta.iva} | Total: ${venta.total}")
                            Text("Efectivo: ${venta.montoEfectivo} | Saldo: ${venta.montoUsadoSaldo} | Cambio: ${venta.cambio}")
                            venta.detalles.forEach { d ->
                                Text("- ${d.medicamentoNombre} (${d.presentacion}) x${d.cantidad} = ${d.subtotal}")
                            }
                        }
                    }
                }
            }
        }
    }
}