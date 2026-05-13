package com.sanidad.movil.presentation.screens.dashboard

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
import com.sanidad.movil.data.repository.DashboardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    dashboardRepository: DashboardRepository = remember { DashboardRepository() }
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(dashboardRepository) as T
            }
        }
    )

    val dashboard by viewModel.dashboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Salir") }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            dashboard?.let { d ->
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp)
                ) {
                    item { Text("Ventas del día: ${d.ventasDelDia.cantidadVentas} - Total: C$${String.format("%.2f", d.ventasDelDia.totalVentas)}") }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { Text("Productos más rentables:", style = MaterialTheme.typography.titleLarge) }
                    items(d.productosMasRentables) { prod ->
                        Text("${prod.nombre}: C$${String.format("%.2f", prod.ingresos)}")
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { Text("Productos bajo stock:", style = MaterialTheme.typography.titleLarge) }
                    items(d.productosBajoStock) { prod ->
                        Text("${prod.nombre}: ${prod.stockTotal} unidades")
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { Text("Ranking vendedores:", style = MaterialTheme.typography.titleLarge) }
                    items(d.rankingVendedores) { v ->
                        Text("${v.username}: ${v.cantidadVentas} ventas, C$${String.format("%.2f", v.totalVentas)}")
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { Text("Ventas este mes: C$${String.format("%.2f", d.ventasMesActual)}") }
                    item { Text("Ventas mes anterior: C$${String.format("%.2f", d.ventasMesAnterior)}") }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { Text("Accesos rápidos:", style = MaterialTheme.typography.titleLarge) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onNavigate("medicamentos") }) { Text("Medicamentos") }
                            TextButton(onClick = { onNavigate("ventas") }) { Text("Ventas") }
                            TextButton(onClick = { onNavigate("clientes") }) { Text("Clientes") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onNavigate("usuarios") }) { Text("Usuarios") }
                            TextButton(onClick = { onNavigate("proveedores") }) { Text("Proveedores") }
                            TextButton(onClick = { onNavigate("lotes") }) { Text("Lotes") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onNavigate("recetas") }) { Text("Recetas") }
                            TextButton(onClick = { onNavigate("racks") }) { Text("Racks") }
                            TextButton(onClick = { onNavigate("ubicaciones") }) { Text("Ubicaciones") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onNavigate("devoluciones") }) { Text("Devoluciones") }
                            TextButton(onClick = { onNavigate("devoluciones_proveedor") }) { Text("Dev. Proveed.") }
                            TextButton(onClick = { onNavigate("alertas") }) { Text("Alertas") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onNavigate("perdidas") }) { Text("Pérdidas") }
                            TextButton(onClick = { onNavigate("recomendaciones") }) { Text("Recomend.") }
                        }
                    }
                }
            } ?: Text("No se pudo cargar el dashboard", modifier = Modifier.padding(padding))
        }
    }
}