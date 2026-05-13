package com.sanidad.movil.presentation.screens.recetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.RecetaResponse
import com.sanidad.movil.data.repository.RecetaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecetasViewModel(
    private val recetaRepository: RecetaRepository
) : ViewModel() {
    private val _recetas = MutableStateFlow<List<RecetaResponse>>(emptyList())
    val recetas: StateFlow<List<RecetaResponse>> = _recetas
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarRecetas() }

    fun cargarRecetas() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = recetaRepository.getTodasLasRecetas()
            result.fold(
                onSuccess = { _recetas.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}