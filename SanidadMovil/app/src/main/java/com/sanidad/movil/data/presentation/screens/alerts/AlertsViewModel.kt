package com.sanidad.movil.presentation.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.AlertResponse
import com.sanidad.movil.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AlertsUiState(
    val alerts: List<AlertResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String? = null,
    val statusFilter: String = "PENDING" // "PENDING", "RESOLVED", "ALL"
) {
    // KPIs derivados
    val total: Int get() = alerts.size
    val pending: Int get() = alerts.count { it.status == "PENDING" }
    val resolved: Int get() = alerts.count { it.status == "ACKNOWLEDGED" || it.status == "RESOLVED" }
    val high: Int get() = alerts.count { it.severity == "ALTA" || it.severity == "CRITICAL" }

    // Lista filtrada según el estado seleccionado
    val filteredAlerts: List<AlertResponse> get() = when (statusFilter) {
        "PENDING" -> alerts.filter { it.status == "PENDING" }
        "RESOLVED" -> alerts.filter { it.status == "ACKNOWLEDGED" || it.status == "RESOLVED" }
        else -> alerts
    }
}

class AlertsViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState(isLoading = true))
    val uiState: StateFlow<AlertsUiState> = _uiState

    init { cargarAlertas() }

    fun cargarAlertas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = alertRepository.getAlertas()) {
                is ApiResult.Success -> {
                    val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    _uiState.value = _uiState.value.copy(
                        alerts = result.data,
                        isLoading = false,
                        error = null,
                        lastUpdated = now
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        alerts = emptyList()
                    )
                }
                is ApiResult.Loading -> { /* no ocurre aquí */ }
            }
        }
    }

    fun atenderAlerta(id: Long) {
        viewModelScope.launch {
            when (val result = alertRepository.reconocerAlerta(id)) {
                is ApiResult.Success -> cargarAlertas()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = "No se pudo atender la alerta.")
                else -> {}
            }
        }
    }

    fun generarAlertas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = alertRepository.generarAlertas()) {
                is ApiResult.Success -> cargarAlertas()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al generar alertas."
                )
                else -> {}
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}