package com.sanidad.movil.presentation.screens.perdidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.PerdidasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class PerdidasUiState(
    val vencidos: List<ProductoVencidoDTO> = emptyList(),
    val inmoviles: List<ProductoInmovilDTO> = emptyList(),
    val inconsistencias: List<InconsistenciaStockDTO> = emptyList(),
    val resumen: ResumenPerdidasDTO? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeTab: Int = 0 // 0 = vencidos, 1 = inmoviles, 2 = inconsistencias
)

class PerdidasViewModel(
    private val perdidasRepo: PerdidasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerdidasUiState())
    val uiState: StateFlow<PerdidasUiState> = _uiState

    init { cargarDatos() }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val vencidosResult = perdidasRepo.getProductosVencidos()
            val inmovilesResult = perdidasRepo.getProductosInmoviles()
            val inconsistenciasResult = perdidasRepo.getInconsistenciasStock()
            val resumenResult = perdidasRepo.getResumenPerdidas()

            val vencidos = when (vencidosResult) {
                is ApiResult.Success -> vencidosResult.data
                else -> emptyList()
            }
            val inmoviles = when (inmovilesResult) {
                is ApiResult.Success -> inmovilesResult.data
                else -> emptyList()
            }
            val inconsistencias = when (inconsistenciasResult) {
                is ApiResult.Success -> inconsistenciasResult.data
                else -> emptyList()
            }
            val resumen = when (resumenResult) {
                is ApiResult.Success -> resumenResult.data
                else -> null
            }

            val allFailed = vencidosResult is ApiResult.Error &&
                    inmovilesResult is ApiResult.Error &&
                    inconsistenciasResult is ApiResult.Error &&
                    resumenResult is ApiResult.Error

            _uiState.value = if (allFailed) {
                _uiState.value.copy(
                    isLoading = false,
                    error = "Error al sincronizar el análisis de pérdidas operativas."
                )
            } else {
                _uiState.value.copy(
                    vencidos = vencidos,
                    inmoviles = inmoviles,
                    inconsistencias = inconsistencias,
                    resumen = resumen,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "NI"))
        return format.format(value)
    }
}