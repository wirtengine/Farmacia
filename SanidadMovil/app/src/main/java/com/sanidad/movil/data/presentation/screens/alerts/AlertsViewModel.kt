package com.sanidad.movil.data.presentation.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.AlertResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS ======================
    private val _alerts = MutableStateFlow<List<AlertResponse>>(emptyList())
    val alerts: StateFlow<List<AlertResponse>> = _alerts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated

    // ====================== FILTRO ======================
    private val _statusFilter = MutableStateFlow("PENDING") // PENDING, RESOLVED, ALL
    val statusFilter: StateFlow<String> = _statusFilter

    // ====================== KPIs ======================
    val total: Int get() = _alerts.value.size
    val pending: Int get() = _alerts.value.count { it.status == "PENDING" }
    val resolved: Int get() = _alerts.value.count { it.status == "ACKNOWLEDGED" || it.status == "RESOLVED" }
    val high: Int get() = _alerts.value.count { it.severity == "ALTA" || it.severity == "CRITICAL" }

    // ====================== ALERTAS FILTRADAS ======================
    val filteredAlerts: List<AlertResponse>
        get() {
            return when (_statusFilter.value) {
                "PENDING" -> _alerts.value.filter { it.status == "PENDING" }
                "RESOLVED" -> _alerts.value.filter { it.status == "ACKNOWLEDGED" || it.status == "RESOLVED" }
                else -> _alerts.value // ALL
            }
        }

    // ====================== CARGA DE DATOS ======================
    fun cargarAlertas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.obtenerAlertas()
                if (response.isSuccessful) {
                    _alerts.value = response.body() ?: emptyList()
                    _lastUpdated.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                } else {
                    _error.value = "Error al cargar alertas"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión con el centro de control."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    // ====================== ATENDER ALERTA ======================
    fun atenderAlerta(id: Long) {
        viewModelScope.launch {
            try {
                api.reconocerAlerta(id)
                cargarAlertas()
            } catch (_: Exception) {
                _error.value = "No se pudo procesar la alerta."
            }
        }
    }

    // ====================== GENERAR ALERTAS ======================
    fun generarAlertas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.generarAlertas()
                cargarAlertas()
            } catch (_: Exception) {
                _error.value = "Error al generar nuevas alertas."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStatusText(status: String): String {
        return when (status) {
            "PENDING" -> "Pendiente"
            "ACKNOWLEDGED" -> "Atendida"
            "RESOLVED" -> "Resuelta"
            else -> status
        }
    }
}