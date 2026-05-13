package com.sanidad.movil.presentation.screens.usuarios

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
import com.sanidad.movil.data.repository.UsuarioRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(
    usuarioRepository: UsuarioRepository = remember { UsuarioRepository() }
) {
    val viewModel: UsuariosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return UsuariosViewModel(usuarioRepository) as T
            }
        }
    )
    val usuarios by viewModel.usuarios.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Usuarios") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(usuarios) { user ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(user.username, style = MaterialTheme.typography.titleLarge)
                            Text("Rol: ${user.rol}")
                        }
                    }
                }
            }
        }
    }
}