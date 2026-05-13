package com.sanidad.movil.presentation.screens.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.ClienteResponse
import com.sanidad.movil.data.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientesViewModel(
    private val clienteRepository: ClienteRepository
) : ViewModel() {
    private val _clientes = MutableStateFlow<List<ClienteResponse>>(emptyList())
    val clientes: StateFlow<List<ClienteResponse>> = _clientes
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarClientes() }

    fun cargarClientes() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = clienteRepository.getClientes()
            result.fold(
                onSuccess = { _clientes.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}