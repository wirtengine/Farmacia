package com.sanidad.movil.presentation.screens.devoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.DevolucionResponse
import com.sanidad.movil.data.repository.DevolucionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DevolucionesViewModel(
    private val devolucionRepository: DevolucionRepository
) : ViewModel() {
    private val _devoluciones = MutableStateFlow<List<DevolucionResponse>>(emptyList())
    val devoluciones: StateFlow<List<DevolucionResponse>> = _devoluciones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarDevoluciones() }

    fun cargarDevoluciones() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = devolucionRepository.getDevoluciones()
            result.fold(
                onSuccess = { _devoluciones.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}