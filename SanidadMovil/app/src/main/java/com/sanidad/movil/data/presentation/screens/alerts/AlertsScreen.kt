package com.sanidad.movil.presentation.screens.alerts

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
import com.sanidad.movil.data.repository.AlertRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    alertRepository: AlertRepository = remember { AlertRepository() }
) {
    val viewModel: AlertsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AlertsViewModel(alertRepository) as T
            }
        }
    )
    val alertas by viewModel.alertas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Alertas") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(alertas) { alert ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${alert.type} (${alert.severity}) - ${alert.status}", style = MaterialTheme.typography.titleLarge)
                            Text(alert.message)
                            Text("Creada: ${alert.createdAt}")
                        }
                    }
                }
            }
        }
    }
}