package com.sanidad.movil.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.DashboardResponseDTO
import com.sanidad.movil.data.repository.AlertRepository
import com.sanidad.movil.data.repository.DashboardRepository
import com.sanidad.movil.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val dashboard: DashboardResponseDTO? = null,
    val pendingAlerts: Int = 0,
    val pendingRecs: Int = 0,
    val lastUpdated: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val dashboardRepo: DashboardRepository,
    private val alertsRepo: AlertRepository,
    private val recsRepo: RecommendationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState(isLoading = true))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val dashResult = dashboardRepo.obtenerDashboard()
            val alertsResult = alertsRepo.getAlertas()
            val recsResult = recsRepo.getRecomendaciones()

            val dashboardData = when (dashResult) {
                is ApiResult.Success -> dashResult.data
                else -> null
            }
            val alertsCount = when (alertsResult) {
                is ApiResult.Success -> alertsResult.data.count { it.status == "PENDING" }
                else -> 0
            }
            val recsCount = when (recsResult) {
                is ApiResult.Success -> recsResult.data.size
                else -> 0
            }

            val allFailed = dashResult is ApiResult.Error &&
                    alertsResult is ApiResult.Error &&
                    recsResult is ApiResult.Error

            _state.value = if (allFailed) {
                _state.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar la información"
                )
            } else {
                val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _state.value.copy(
                    dashboard = dashboardData,
                    pendingAlerts = alertsCount,
                    pendingRecs = recsCount,
                    lastUpdated = now,
                    isLoading = false,
                    error = null
                )
            }
        }
    }
}