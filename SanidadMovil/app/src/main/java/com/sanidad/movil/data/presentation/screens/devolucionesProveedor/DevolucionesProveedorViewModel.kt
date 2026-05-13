package com.sanidad.movil.presentation.screens.devolucionesProveedor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.DevolucionProveedorResponse
import com.sanidad.movil.data.repository.DevolucionProveedorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DevolucionesProveedorViewModel(
    private val repository: DevolucionProveedorRepository
) : ViewModel() {
    private val _devoluciones = MutableStateFlow<List<DevolucionProveedorResponse>>(emptyList())
    val devoluciones: StateFlow<List<DevolucionProveedorResponse>> = _devoluciones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarDevoluciones() }

    fun cargarDevoluciones() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getDevoluciones()
            result.fold(
                onSuccess = { _devoluciones.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}