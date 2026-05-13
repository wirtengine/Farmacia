package com.sanidad.movil.presentation.screens.recommendations

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
import com.sanidad.movil.data.repository.RecommendationRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    recommendationRepository: RecommendationRepository = remember { RecommendationRepository() }
) {
    val viewModel: RecommendationsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RecommendationsViewModel(recommendationRepository) as T
            }
        }
    )
    val recomendaciones by viewModel.recomendaciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Recomendaciones") }) }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(recomendaciones) { r ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${r.type} (${r.priority}) - ${r.status}", style = MaterialTheme.typography.titleLarge)
                            Text(r.message)
                            Text("Creada: ${r.createdAt}")
                        }
                    }
                }
            }
        }
    }
}