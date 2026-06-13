package com.sanidad.movil.data.presentation.screens.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS ======================
    private val _recommendations = MutableStateFlow<List<RecommendationResponse>>(emptyList())
    val recommendations: StateFlow<List<RecommendationResponse>> = _recommendations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated

    // ====================== FILTRO ======================
    private val _statusFilter = MutableStateFlow("PENDING") // PENDING, ACCEPTED, DISMISSED, ALL
    val statusFilter: StateFlow<String> = _statusFilter

    // ====================== KPIs ======================
    val total: Int get() = _recommendations.value.size
    val pending: Int get() = _recommendations.value.count { it.status == "PENDING" }
    val accepted: Int get() = _recommendations.value.count { it.status == "ACCEPTED" || it.status == "RESOLVED" }
    val dismissed: Int get() = _recommendations.value.count { it.status == "DISMISSED" }

    // ====================== RECOMENDACIONES FILTRADAS ======================
    val filteredRecs: List<RecommendationResponse>
        get() {
            return when (_statusFilter.value) {
                "ALL" -> _recommendations.value
                else -> _recommendations.value.filter { it.status == _statusFilter.value }
            }
        }

    // ====================== CARGA DE DATOS ======================
    fun cargarRecomendaciones() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.obtenerRecomendaciones()
                if (response.isSuccessful) {
                    _recommendations.value = response.body() ?: emptyList()
                    _lastUpdated.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                } else {
                    _error.value = "Error al cargar recomendaciones"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión con el motor de recomendaciones."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    // ====================== ACEPTAR RECOMENDACIÓN ======================
    fun aceptarRecomendacion(id: Long) {
        viewModelScope.launch {
            try {
                api.aceptarRecomendacion(id)
                cargarRecomendaciones()
            } catch (_: Exception) {
                _error.value = "No se pudo aceptar la recomendación."
            }
        }
    }

    // ====================== DESCARTAR RECOMENDACIÓN ======================
    fun descartarRecomendacion(id: Long) {
        viewModelScope.launch {
            try {
                api.descartarRecomendacion(id)
                cargarRecomendaciones()
            } catch (_: Exception) {
                _error.value = "No se pudo descartar la recomendación."
            }
        }
    }

    // ====================== GENERAR RECOMENDACIONES ======================
    fun generarRecomendaciones() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.generarRecomendaciones()
                cargarRecomendaciones()
            } catch (_: Exception) {
                _error.value = "Error al generar nuevas recomendaciones."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ====================== HELPERS ======================
    fun getPriorityLabel(priority: String): String {
        return when (priority.uppercase()) {
            "HIGH" -> "Alta"
            "MEDIUM" -> "Media"
            "LOW" -> "Baja"
            else -> priority
        }
    }

    fun getStatusDisplay(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "Pendiente"
            "ACCEPTED" -> "Aceptada"
            "RESOLVED" -> "Resuelta"
            "DISMISSED" -> "Descartada"
            else -> status
        }
    }

    fun getTypeLabel(type: String): String {
        return when (type.uppercase()) {
            "PURCHASE_SUGGESTION" -> "Compra"
            "AVOID_RESTOCK" -> "Stock"
            "PRIORITIZE_SALE" -> "Venta"
            else -> type
        }
    }
}