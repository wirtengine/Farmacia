package com.sanidad.movil.data.presentation.screens.perdidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PerdidasViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    private val _vencidos = MutableStateFlow<List<ProductoVencidoDTO>>(emptyList())
    val vencidos: StateFlow<List<ProductoVencidoDTO>> = _vencidos

    private val _inmoviles = MutableStateFlow<List<ProductoInmovilDTO>>(emptyList())
    val inmoviles: StateFlow<List<ProductoInmovilDTO>> = _inmoviles

    private val _inconsistencias = MutableStateFlow<List<InconsistenciaStockDTO>>(emptyList())
    val inconsistencias: StateFlow<List<InconsistenciaStockDTO>> = _inconsistencias

    private val _resumen = MutableStateFlow<ResumenPerdidasDTO?>(null)
    val resumen: StateFlow<ResumenPerdidasDTO?> = _resumen

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resVencidos = api.obtenerProductosVencidos()
                val resInmoviles = api.obtenerProductosInmoviles()
                val resInconsistencias = api.obtenerInconsistenciasStock()
                val resResumen = api.obtenerResumenPerdidas()

                _vencidos.value = resVencidos.body() ?: emptyList()
                _inmoviles.value = resInmoviles.body() ?: emptyList()
                _inconsistencias.value = resInconsistencias.body() ?: emptyList()
                _resumen.value = resResumen.body()
            } catch (e: Exception) {
                _error.value = "Error al sincronizar el análisis de pérdidas operativas."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun formatCurrency(value: Double): String {
        return "C$ ${String.format("%.2f", value)}"
    }
}