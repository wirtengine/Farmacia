package com.sanidad.movil.presentation.screens.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.VentaResponse
import com.sanidad.movil.data.repository.VentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VentasViewModel(
    private val ventaRepository: VentaRepository
) : ViewModel() {
    private val _ventas = MutableStateFlow<List<VentaResponse>>(emptyList())
    val ventas: StateFlow<List<VentaResponse>> = _ventas
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarVentas() }

    fun cargarVentas() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = ventaRepository.getVentas()
            result.fold(
                onSuccess = { _ventas.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}