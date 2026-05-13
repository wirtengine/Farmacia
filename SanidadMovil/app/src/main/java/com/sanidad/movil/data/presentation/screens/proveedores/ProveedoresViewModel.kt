package com.sanidad.movil.presentation.screens.proveedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.ProveedorResponse
import com.sanidad.movil.data.repository.ProveedorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProveedoresViewModel(
    private val proveedorRepository: ProveedorRepository
) : ViewModel() {
    private val _proveedores = MutableStateFlow<List<ProveedorResponse>>(emptyList())
    val proveedores: StateFlow<List<ProveedorResponse>> = _proveedores
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarProveedores() }

    fun cargarProveedores() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = proveedorRepository.getProveedores()
            result.fold(
                onSuccess = { _proveedores.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}