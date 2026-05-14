package com.sanidad.movil.data.presentation.screens.ventas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaCreateScreen(
    usuarioId: Long,
    onVentaExitosa: () -> Unit,
    onCancelar: () -> Unit
) {

    val viewModel: VentaCreateViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.usuarioId = usuarioId
        viewModel.cargarDatosIniciales()
    }

    val carrito by viewModel.carrito.collectAsState()
    val query by viewModel.query.collectAsState()
    val resultadosBusqueda by viewModel.resultadosBusqueda.collectAsState()
    val loteFIFO by viewModel.loteFIFO.collectAsState()
    val complementarios by viewModel.complementarios.collectAsState()
    val clienteSeleccionado by viewModel.clienteSeleccionado.collectAsState()
    val recetaSeleccionada by viewModel.recetaSeleccionada.collectAsState()
    val montoEfectivo by viewModel.montoEfectivo.collectAsState()
    val montoUsadoSaldo by viewModel.montoUsadoSaldo.collectAsState()
    val tipoVenta by viewModel.tipoVenta.collectAsState()

    val clientes by viewModel.clientes.collectAsState()
    val recetasDisponibles by viewModel.recetasDisponibles.collectAsState()

    val total = viewModel.total
    val cambio = viewModel.cambio
    val requiereReceta = viewModel.requiereReceta

    var mostrarDialogoCliente by remember {
        mutableStateOf(false)
    }

    var mostrarDialogoReceta by remember {
        mutableStateOf(false)
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Nueva Venta")
                },

                actions = {

                    TextButton(
                        onClick = onCancelar
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        },

        bottomBar = {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Total:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "C$${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (cambio > 0) {

                        Text(
                            text = "Cambio: C$${String.format("%.2f", cambio)}",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        if (tipoVenta == "cliente") {

                            OutlinedTextField(
                                value = montoUsadoSaldo.toInt().toString(),

                                onValueChange = {
                                    val monto = it.toDoubleOrNull() ?: 0.0
                                    viewModel.setMontoUsadoSaldo(monto)
                                },

                                label = {
                                    Text("Usar saldo")
                                },

                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),

                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = montoEfectivo.toInt().toString(),

                            onValueChange = {
                                val monto = it.toDoubleOrNull() ?: 0.0
                                viewModel.setMontoEfectivo(monto)
                            },

                            label = {
                                Text("Efectivo")
                            },

                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),

                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(

                        onClick = {

                            viewModel.crearVenta(

                                onSuccess = {
                                    onVentaExitosa()
                                },

                                onError = {

                                }
                            )
                        },

                        modifier = Modifier.fillMaxWidth(),

                        enabled = carrito.isNotEmpty()
                    ) {

                        Text("Confirmar y Pagar")
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                FilterChip(
                    selected = tipoVenta == "rapida",

                    onClick = {
                        viewModel.seleccionarCliente(null)
                    },

                    label = {
                        Text("Rápida")
                    },

                    modifier = Modifier.padding(end = 8.dp)
                )

                FilterChip(
                    selected = tipoVenta == "cliente",

                    onClick = {
                        mostrarDialogoCliente = true
                    },

                    label = {
                        Text("A Cliente")
                    }
                )
            }

            if (clienteSeleccionado != null) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column {

                            Text(
                                text = clienteSeleccionado!!.nombre,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Saldo: C$${String.format("%.2f", clienteSeleccionado!!.saldo)}"
                            )
                        }

                        TextButton(
                            onClick = {
                                mostrarDialogoCliente = true
                            }
                        ) {
                            Text("Cambiar")
                        }
                    }
                }
            }

            OutlinedTextField(

                value = query,

                onValueChange = {
                    viewModel.buscarMedicamento(it)
                },

                label = {
                    Text("Buscar medicamento...")
                },

                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            loteFIFO?.let { fifo ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "⚡ Lote recomendado (próximo a vencer)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Text(
                                text = fifo.medicamentoNombre,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Stock: ${fifo.stockVenta} | Precio: C$${fifo.precioUnitario}"
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.agregarAlCarrito(fifo)
                            }
                        ) {
                            Text("Agregar")
                        }
                    }
                }
            }

            if (resultadosBusqueda.isNotEmpty()) {

                LazyColumn(
                    modifier = Modifier.weight(0.3f),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {

                    items(resultadosBusqueda) { item ->

                        ListItem(

                            headlineContent = {
                                Text(item.medicamentoNombre)
                            },

                            supportingContent = {
                                Text(
                                    "Stock: ${item.stockVenta} | ${item.loteNum ?: ""}"
                                )
                            },

                            trailingContent = {
                                Text(
                                    "C$${item.precioUnitario}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },

                            modifier = Modifier.clickable {
                                viewModel.agregarAlCarrito(item)
                            }
                        )
                    }
                }
            }

            if (complementarios.isNotEmpty()) {

                Text(
                    text = "🛒 Productos complementarios",
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (requiereReceta) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "⚠️ Requiere receta validada",
                            color = Color(0xFFE65100)
                        )

                        TextButton(
                            onClick = {
                                mostrarDialogoReceta = true
                            }
                        ) {

                            Text(
                                if (recetaSeleccionada != null)
                                    "Cambiar"
                                else
                                    "Seleccionar"
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {

                itemsIndexed(carrito) { index, item ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {

                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = item.medicamentoNombre,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "C$${item.precioUnitario} x ${item.cantidad} = C$${String.format("%.2f", item.precioUnitario * item.cantidad)}"
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                IconButton(
                                    onClick = {
                                        if (item.cantidad > 1) {
                                            viewModel.actualizarCantidadCarrito(
                                                index,
                                                item.cantidad - 1
                                            )
                                        }
                                    }
                                ) {
                                    Text("-")
                                }

                                Text("${item.cantidad}")

                                IconButton(
                                    onClick = {
                                        viewModel.actualizarCantidadCarrito(
                                            index,
                                            item.cantidad + 1
                                        )
                                    }
                                ) {
                                    Text("+")
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.eliminarDelCarrito(index)
                                    }
                                ) {

                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCliente) {

        AlertDialog(

            onDismissRequest = {
                mostrarDialogoCliente = false
            },

            title = {
                Text("Seleccionar cliente")
            },

            text = {

                LazyColumn {

                    items(clientes) { cliente ->

                        ListItem(

                            headlineContent = {
                                Text(cliente.nombre)
                            },

                            supportingContent = {
                                Text("Cédula: ${cliente.cedula}")
                            },

                            modifier = Modifier.clickable {

                                viewModel.seleccionarCliente(cliente)

                                mostrarDialogoCliente = false
                            }
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        mostrarDialogoCliente = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoReceta) {

        AlertDialog(

            onDismissRequest = {
                mostrarDialogoReceta = false
            },

            title = {
                Text("Seleccionar receta")
            },

            text = {

                LazyColumn {

                    items(recetasDisponibles) { receta ->

                        ListItem(

                            headlineContent = {
                                Text(receta.codigoMinsa ?: "Sin código")
                            },

                            supportingContent = {
                                Text("Farm. ${receta.farmaceuticoUsername}")
                            },

                            modifier = Modifier.clickable {

                                viewModel.seleccionarReceta(receta)

                                mostrarDialogoReceta = false
                            }
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        mostrarDialogoReceta = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}