package com.sanidad.movil.presentation.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.AlertResponse
import com.sanidad.movil.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {
    private val _alertas = MutableStateFlow<List<AlertResponse>>(emptyList())
    val alertas: StateFlow<List<AlertResponse>> = _alertas
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarAlertas() }

    fun cargarAlertas() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = alertRepository.getAlertas()
            result.fold(
                onSuccess = { _alertas.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}