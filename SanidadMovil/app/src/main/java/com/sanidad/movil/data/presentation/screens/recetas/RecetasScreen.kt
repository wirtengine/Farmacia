package com.sanidad.movil.data.presentation.screens.recetas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetasScreen(viewModel: RecetasViewModel = viewModel()) {
    val recetas by viewModel.recetas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val mostrarTodas by viewModel.mostrarTodas.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val codigoMinsa by viewModel.codigoMinsa.collectAsState()

    val esAdmin = UserSession.rol == "ADMIN"
    val esFarmaceutico = UserSession.rol == "FARMACEUTICO"
    val farmaceuticoId = UserSession.userId
    val puedeValidar = esAdmin || esFarmaceutico

    LaunchedEffect(mostrarTodas) {
        viewModel.cargarRecetas(esAdmin, farmaceuticoId)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }

    // Diálogo de confirmación para validar
    var showConfirmDialog by remember { mutableStateOf(false) }
    var recetaIdToAction by remember { mutableStateOf<Long?>(null) }
    var aprobarAction by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recetas") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Sección de subida
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subir nueva receta", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedImageUri?.lastPathSegment ?: "Seleccionar imagen")
                        }
                        if (selectedImageUri != null) {
                            IconButton(onClick = { viewModel.setImageUri(null) }) {
                                Icon(Icons.Default.Close, "Quitar imagen")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codigoMinsa,
                        onValueChange = { viewModel.setCodigoMinsa(it) },
                        label = { Text("Código MINSA") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.subirReceta(
                                farmaceuticoId = farmaceuticoId,
                                onSuccess = {
                                    viewModel.cargarRecetas(esAdmin, farmaceuticoId)
                                },
                                onError = { /* mostrar error */ }
                            )
                        },
                        enabled = selectedImageUri != null && codigoMinsa.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Subir Receta") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle y listado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (mostrarTodas) "Historial completo" else if (esAdmin) "Recetas pendientes" else "Mis recetas",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { viewModel.toggleMostrarTodas() }) {
                    Text(if (mostrarTodas) "Ver pendientes/propias" else "Ver todas")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (recetas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron recetas.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(recetas) { receta ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Imagen (si existe)
                                if (receta.imagenUrl != null) {
                                    AsyncImage(
                                        model = receta.imagenUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text("Código MINSA: ${receta.codigoMinsa ?: "N/A"}", fontWeight = FontWeight.Bold)
                                Text("Farmacéutico: ${receta.farmaceuticoUsername ?: ""}")
                                Text("Fecha: ${receta.fechaSubida?.let { formatearFecha(it) } ?: ""}")
                                Text("Estado: ${receta.estado}", color = when (receta.estado) {
                                    "PENDIENTE" -> Color(0xFFFFA000)
                                    "APROBADA" -> Color(0xFF4CAF50)
                                    "RECHAZADA" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                })

                                if (receta.ventaId != null) {
                                    Text("Venta #${receta.ventaId}", color = Color.Blue)
                                }

                                if (puedeValidar && receta.estado == "PENDIENTE") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = {
                                            recetaIdToAction = receta.id
                                            aprobarAction = true
                                            showConfirmDialog = true
                                        }) { Text("Aprobar", color = Color(0xFF4CAF50)) }
                                        TextButton(onClick = {
                                            recetaIdToAction = receta.id
                                            aprobarAction = false
                                            showConfirmDialog = true
                                        }) { Text("Rechazar", color = Color(0xFFF44336)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(if (aprobarAction) "Aprobar receta" else "Rechazar receta") },
            text = { Text(if (aprobarAction) "¿Desea aprobar esta receta?" else "¿Desea rechazar esta receta?") },
            confirmButton = {
                TextButton(onClick = {
                    recetaIdToAction?.let {
                        viewModel.validarReceta(
                            recetaId = it,
                            aprobar = aprobarAction,
                            farmaceuticoId = farmaceuticoId
                        ) {
                            viewModel.cargarRecetas(esAdmin, farmaceuticoId)
                            showConfirmDialog = false
                        }
                    }
                }) { Text(if (aprobarAction) "Aprobar" else "Rechazar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }
}

fun formatearFecha(fecha: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        date?.let { outputFormat.format(it) } ?: fecha
    } catch (e: Exception) {
        fecha
    }
}