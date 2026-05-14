package com.sanidad.movil.data.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    private val _dashboardData = MutableStateFlow<DashboardResponseDTO?>(null)
    val dashboardData: StateFlow<DashboardResponseDTO?> = _dashboardData

    private val _pendingAlerts = MutableStateFlow(0)
    val pendingAlerts: StateFlow<Int> = _pendingAlerts

    private val _pendingRecs = MutableStateFlow(0)
    val pendingRecs: StateFlow<Int> = _pendingRecs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated

    init { cargarDatos() }

    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dashboardRes = api.obtenerDashboard()
                val alertsRes = api.obtenerAlertas()
                val recsRes = api.obtenerRecomendaciones()

                _dashboardData.value = dashboardRes.body()
                _pendingAlerts.value = alertsRes.body()?.filter { it.status == "PENDING" }?.size ?: 0
                _pendingRecs.value = recsRes.body()?.size ?: 0
                _lastUpdated.value = java.text.SimpleDateFormat(
                    "HH:mm:ss", java.util.Locale.getDefault()
                ).format(java.util.Date())
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al actualizar datos"
            } finally {
                _isLoading.value = false
            }
        }
    }
}