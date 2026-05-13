package com.sanidad.movil.presentation.screens.ubicaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.UbicacionLoteResponse
import com.sanidad.movil.data.repository.UbicacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UbicacionesViewModel(
    private val ubicacionRepository: UbicacionRepository
) : ViewModel() {
    private val _ubicaciones = MutableStateFlow<List<UbicacionLoteResponse>>(emptyList())
    val ubicaciones: StateFlow<List<UbicacionLoteResponse>> = _ubicaciones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarUbicaciones() }

    fun cargarUbicaciones() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = ubicacionRepository.getTodasUbicaciones()
            result.fold(
                onSuccess = { _ubicaciones.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}