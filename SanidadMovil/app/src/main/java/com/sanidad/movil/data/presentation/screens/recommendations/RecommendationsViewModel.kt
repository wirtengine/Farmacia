package com.sanidad.movil.presentation.screens.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import com.sanidad.movil.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RecommendationsUiState(
    val recommendations: List<RecommendationResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String? = null,
    val statusFilter: String = "PENDING" // PENDING, ACCEPTED, DISMISSED, ALL
) {
    // KPIs derivados del estado normalizado
    val total: Int get() = recommendations.size
    val pending: Int get() = recommendations.count { normalizeStatus(it.status) == "PENDING" }
    val accepted: Int get() = recommendations.count { normalizeStatus(it.status) == "ACCEPTED" }
    val dismissed: Int get() = recommendations.count { normalizeStatus(it.status) == "DISMISSED" }

    // Lista filtrada según el estado seleccionado
    val filteredRecs: List<RecommendationResponse> get() = when (statusFilter) {
        "ALL" -> recommendations
        else -> {
            val filterNorm = statusFilter.uppercase().trim()
            recommendations.filter { normalizeStatus(it.status) == filterNorm }
        }
    }

    companion object {
        /** Normaliza estado: ACCEPTED/RESOLVED -> ACCEPTED, DISCARDED -> DISMISSED */
        fun normalizeStatus(status: String?): String {
            if (status == null) return "UNKNOWN"
            val s = status.uppercase().trim()
            return when {
                s == "ACKNOWLEDGED" || s == "RESOLVED" -> "ACCEPTED"
                s == "DISCARDED" -> "DISMISSED"
                else -> s
            }
        }
    }
}

class RecommendationsViewModel(
    private val repository: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState(isLoading = true))
    val uiState: StateFlow<RecommendationsUiState> = _uiState

    init { cargarRecomendaciones() }

    fun cargarRecomendaciones() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getRecomendaciones()) {
                is ApiResult.Success -> {
                    val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    _uiState.value = _uiState.value.copy(
                        recommendations = result.data,
                        isLoading = false,
                        error = null,
                        lastUpdated = now
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        recommendations = emptyList()
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }

    fun aceptarRecomendacion(id: Long) {
        viewModelScope.launch {
            when (val result = repository.aceptarRecomendacion(id)) {
                is ApiResult.Success -> cargarRecomendaciones()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = "No se pudo aceptar la recomendación.")
                else -> {}
            }
        }
    }

    fun descartarRecomendacion(id: Long) {
        viewModelScope.launch {
            when (val result = repository.descartarRecomendacion(id)) {
                is ApiResult.Success -> cargarRecomendaciones()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = "No se pudo descartar la recomendación.")
                else -> {}
            }
        }
    }

    fun generarRecomendaciones() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.generarRecomendaciones()) {
                is ApiResult.Success -> cargarRecomendaciones()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
                else -> {}
            }
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Helpers de presentación
    fun getPriorityLabel(priority: String): String = when (priority.uppercase()) {
        "HIGH" -> "Alta"
        "MEDIUM" -> "Media"
        "LOW" -> "Baja"
        else -> priority
    }

    fun getStatusDisplay(status: String): String = when (status.uppercase().trim()) {
        "PENDING" -> "Pendiente"
        "ACCEPTED", "RESOLVED" -> "Aceptada"
        "DISMISSED", "DISCARDED" -> "Descartada"
        else -> status
    }

    fun getTypeLabel(type: String): String = when (type.uppercase()) {
        "PURCHASE_SUGGESTION" -> "Compra"
        "AVOID_RESTOCK" -> "Stock"
        "PRIORITIZE_SALE" -> "Venta"
        else -> type
    }
}