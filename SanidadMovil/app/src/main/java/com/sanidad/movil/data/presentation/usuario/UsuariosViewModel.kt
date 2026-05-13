package com.sanidad.movil.presentation.screens.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.UsuarioResponse
import com.sanidad.movil.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UsuariosViewModel(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {
    private val _usuarios = MutableStateFlow<List<UsuarioResponse>>(emptyList())
    val usuarios: StateFlow<List<UsuarioResponse>> = _usuarios
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarUsuarios() }

    fun cargarUsuarios() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = usuarioRepository.getUsuarios()
            result.fold(
                onSuccess = { _usuarios.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}