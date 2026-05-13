package com.sanidad.movil.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.DashboardResponseDTO
import com.sanidad.movil.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _dashboard = MutableStateFlow<DashboardResponseDTO?>(null)
    val dashboard: StateFlow<DashboardResponseDTO?> = _dashboard

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarDashboard() }

    fun cargarDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = dashboardRepository.obtenerDashboard()
            result.fold(
                onSuccess = { _dashboard.value = it },
                onFailure = { /* manejar error */ }
            )
            _isLoading.value = false
        }
    }
}