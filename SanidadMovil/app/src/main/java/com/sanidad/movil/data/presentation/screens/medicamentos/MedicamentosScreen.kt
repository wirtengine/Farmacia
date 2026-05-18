package com.sanidad.movil.data.presentation.screens.medicamentos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sanidad.movil.data.UserSession
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentosScreen(
    viewModel: MedicamentosViewModel = viewModel()
) {

    val medicamentos by viewModel.medicamentos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()

    val isAdmin = UserSession.isAdmin()

    LaunchedEffect(Unit) {
        viewModel.cargarMedicamentos()
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Medicamentos")
                }
            )
        },
        floatingActionButton = {

            if (isAdmin) {

                FloatingActionButton(
                    onClick = {
                        viewModel.abrirNuevo()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuevo medicamento"
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.setSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar medicamento...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else {

                val paginatedList = viewModel.paginatedMedicamentos

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {

                    items(paginatedList) { med ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                if (med.imagen != null) {

                                    AsyncImage(
                                        model = med.imagen,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                } else {

                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = med.nombre,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Reg. ${med.registroSanitario}",
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        text = "${med.presentacion} | ${med.via} | ${med.fabricante ?: ""}",
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        text = formatCurrency(med.precioUnitario),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = if (med.activo) "Activo" else "Inactivo",
                                        color =
                                            if (med.activo)
                                                Color(0xFF4CAF50)
                                            else
                                                Color(0xFFF44336)
                                    )
                                }

                                if (isAdmin) {

                                    IconButton(
                                        onClick = {
                                            viewModel.abrirEdicion(med)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar"
                                        )
                                    }

                                    if (med.activo) {

                                        IconButton(
                                            onClick = {

                                                confirmTitle =
                                                    "¿Desactivar medicamento?"

                                                confirmText =
                                                    "El medicamento dejará de estar disponible."

                                                confirmAction = {
                                                    viewModel.desactivarMedicamento(
                                                        med.id
                                                    ) {
                                                        showConfirmDialog = false
                                                    }
                                                }

                                                showConfirmDialog = true
                                            }
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Desactivar"
                                            )
                                        }

                                    } else {

                                        IconButton(
                                            onClick = {

                                                confirmTitle =
                                                    "¿Reactivar medicamento?"

                                                confirmText =
                                                    "Volverá a estar disponible."

                                                confirmAction = {
                                                    viewModel.reactivarMedicamento(
                                                        med.id
                                                    ) {
                                                        showConfirmDialog = false
                                                    }
                                                }

                                                showConfirmDialog = true
                                            }
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reactivar"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TextButton(
                        onClick = {
                            viewModel.setPage(currentPage - 1)
                        },
                        enabled = currentPage > 1
                    ) {
                        Text("← Anterior")
                    }

                    Text("${currentPage} / ${viewModel.totalPages}")

                    TextButton(
                        onClick = {
                            viewModel.setPage(currentPage + 1)
                        },
                        enabled = currentPage < viewModel.totalPages
                    ) {
                        Text("Siguiente →")
                    }
                }
            }
        }
    }

    if (showSheet) {

        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.cerrarSheet()
            },
            sheetState = sheetState
        ) {

            MedicamentoForm(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.cerrarSheet()
                }
            )
        }
    }

    if (showConfirmDialog) {

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            title = {
                Text(confirmTitle)
            },
            text = {
                Text(confirmText)
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        confirmAction()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showConfirmDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun formatCurrency(value: Double): String {

    val format =
        NumberFormat.getCurrencyInstance(
            Locale("es", "NI")
        )

    return format.format(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentoForm(
    viewModel: MedicamentosViewModel,
    onDismiss: () -> Unit
) {

    val form by viewModel.formData.collectAsState()
    val isEdit by viewModel.isEditMode.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            viewModel.setImageUri(uri)
        }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        Text(
            text =
                if (isEdit)
                    "Editar Medicamento"
                else
                    "Nuevo Medicamento",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = form.nombre,
            onValueChange = {
                viewModel.actualizarCampo("nombre", it)
            },
            label = {
                Text("Nombre Comercial *")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.registroSanitario,
            onValueChange = {
                viewModel.actualizarCampo("registroSanitario", it)
            },
            label = {
                Text("Reg. Sanitario *")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEdit
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.fabricante,
            onValueChange = {
                viewModel.actualizarCampo("fabricante", it)
            },
            label = {
                Text("Fabricante *")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = form.precioUnitario,
            onValueChange = {
                viewModel.actualizarCampo("precioUnitario", it)
            },
            label = {
                Text("Precio Unitario *")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = form.receta,
                onCheckedChange = {
                    viewModel.actualizarCampo("receta", it)
                }
            )

            Text("Requiere receta médica")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                launcher.launch("image/*")
            }
        ) {
            Text("Seleccionar imagen")
        }

        imageUri?.let {

            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {

                    viewModel.guardarMedicamento(
                        onSuccess = {
                            onDismiss()
                        },
                        onError = {

                        }
                    )
                }
            ) {
                Text("Guardar")
            }
        }
    }
}