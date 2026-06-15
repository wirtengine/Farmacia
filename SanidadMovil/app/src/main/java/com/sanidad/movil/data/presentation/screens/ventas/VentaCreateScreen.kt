package com.sanidad.movil.presentation.screens.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanidad.movil.data.remote.dto.VentaResponse
import java.text.NumberFormat
import java.util.Locale

// ── Paleta de colores unificada ──
private val Primary = Color(0xFF4F46E5)
private val Success = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val White = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaCreateScreen(
    usuarioId: Long,
    onVentaExitosa: (VentaResponse) -> Unit,
    onCancelar: () -> Unit,
    viewModel: VentaCreateViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.usuarioId = usuarioId
        viewModel.cargarDatosIniciales()
    }

    // Diálogo seleccionar cliente
    if (state.showClienteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClienteDialog(false) },
            title = { Text("Seleccionar cliente", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(state.clientes) { cliente ->
                        ListItem(
                            headlineContent = { Text(cliente.nombre) },
                            supportingContent = { Text("Cédula: ${cliente.cedula}") },
                            modifier = Modifier.clickable { viewModel.seleccionarCliente(cliente) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowClienteDialog(false) }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo seleccionar receta
    if (state.showRecetaDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowRecetaDialog(false) },
            title = { Text("Seleccionar receta", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(state.recetasDisponibles) { receta ->
                        ListItem(
                            headlineContent = { Text(receta.codigoMinsa ?: "Sin código") },
                            supportingContent = { Text("Farm. ${receta.farmaceuticoUsername}") },
                            modifier = Modifier.clickable { viewModel.seleccionarReceta(receta) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowRecetaDialog(false) }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onCancelar) { Text("Cancelar", color = Slate700) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Slate50
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Desglose IVA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 14.sp, color = Slate500)
                        Text(formatCurrency(state.subtotal), fontSize = 14.sp, color = Slate500)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("IVA (15%)", fontSize = 14.sp, color = Slate500)
                        Text(formatCurrency(state.subtotal * 0.15), fontSize = 14.sp, color = Slate500)
                    }
                    HorizontalDivider(color = Slate200, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate900)
                        Text(formatCurrency(state.total), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Primary)
                    }
                    if (state.cambio > 0) {
                        Text(
                            "Cambio: ${formatCurrency(state.cambio)}",
                            color = Success,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Efectivo / Saldo
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.tipoVenta == "cliente") {
                            OutlinedTextField(
                                value = state.montoUsadoSaldo.toInt().toString(),
                                onValueChange = { viewModel.setMontoUsadoSaldo(it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Usar saldo") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        OutlinedTextField(
                            value = state.montoEfectivo.toInt().toString(),
                            onValueChange = { viewModel.setMontoEfectivo(it.toDoubleOrNull() ?: 0.0) },
                            label = { Text("Efectivo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.crearVenta(onSuccess = onVentaExitosa) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.carrito.isNotEmpty() && !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success)
                    ) {
                        Text("Confirmar y Pagar", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tipo de venta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.tipoVenta == "rapida",
                    onClick = { viewModel.seleccionarCliente(null) },
                    label = { Text("Rápida", fontSize = 14.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
                FilterChip(
                    selected = state.tipoVenta == "cliente",
                    onClick = { viewModel.setShowClienteDialog(true) },
                    label = { Text("A Cliente", fontSize = 14.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
            }

            // Cliente seleccionado
            state.clienteSeleccionado?.let { cliente ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cliente.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Saldo: ${formatCurrency(cliente.saldo)}", fontSize = 13.sp, color = Slate500)
                        }
                        TextButton(onClick = { viewModel.setShowClienteDialog(true) }) {
                            Text("Cambiar", color = Primary)
                        }
                    }
                }
            }

            // Búsqueda
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.buscarMedicamento(it) },
                placeholder = { Text("Buscar medicamento...", fontSize = 14.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate200,
                    focusedBorderColor = Primary
                )
            )

            // Lote FIFO recomendado
            state.loteFIFO?.let { fifo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF9E8)),
                    border = BorderStroke(1.dp, Warning),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("⚡ Lote recomendado (próximo a vencer)", fontSize = 11.sp, color = Slate500)
                            Text(fifo.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Stock: ${fifo.stockVenta} | ${formatCurrency(fifo.precioUnitario)}",
                                fontSize = 13.sp,
                                color = Slate700
                            )
                        }
                        Button(
                            onClick = { viewModel.agregarAlCarrito(fifo) },
                            colors = ButtonDefaults.buttonColors(containerColor = Warning),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Agregar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Resultados de búsqueda
            if (state.resultadosBusqueda.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(0.3f),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.resultadosBusqueda) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.medicamentoNombre, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Text(
                                    "${item.loteNum} | Stock: ${item.stockVenta}",
                                    fontSize = 13.sp,
                                    color = Slate500
                                )
                            },
                            trailingContent = {
                                Text(
                                    formatCurrency(item.precioUnitario),
                                    color = Success,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.clickable { viewModel.agregarAlCarrito(item) }
                        )
                    }
                }
            }

            // Requiere receta
            if (state.requiereReceta) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = BorderStroke(1.dp, Warning),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️ Requiere receta validada", color = Color(0xFF9A3412), fontSize = 14.sp)
                        TextButton(onClick = { viewModel.setShowRecetaDialog(true) }) {
                            Text(
                                if (state.recetaSeleccionada != null) "Cambiar" else "Seleccionar",
                                color = Warning
                            )
                        }
                    }
                }
            }

            // Carrito
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(state.carrito) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.medicamentoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${formatCurrency(item.precioUnitario)} x ${item.cantidad} = ${formatCurrency(item.precioUnitario * item.cantidad)}",
                                    fontSize = 13.sp,
                                    color = Slate500
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (item.cantidad > 1)
                                            viewModel.actualizarCantidadCarrito(index, item.cantidad - 1)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("−", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate700)
                                }
                                Text(
                                    "${item.cantidad}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.actualizarCantidadCarrito(index, item.cantidad + 1)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate700)
                                }
                                IconButton(
                                    onClick = { viewModel.eliminarDelCarrito(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        tint = Danger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)