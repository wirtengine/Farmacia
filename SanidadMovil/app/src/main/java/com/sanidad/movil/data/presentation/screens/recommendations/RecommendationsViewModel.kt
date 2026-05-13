package com.sanidad.movil.presentation.screens.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import com.sanidad.movil.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {
    private val _recomendaciones = MutableStateFlow<List<RecommendationResponse>>(emptyList())
    val recomendaciones: StateFlow<List<RecommendationResponse>> = _recomendaciones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarRecomendaciones() }

    fun cargarRecomendaciones() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = recommendationRepository.getRecomendaciones()
            result.fold(
                onSuccess = { _recomendaciones.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}