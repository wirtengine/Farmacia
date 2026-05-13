package com.sanidad.movil.presentation.screens.perdidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.PerdidasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PerdidasViewModel(
    private val perdidasRepository: PerdidasRepository
) : ViewModel() {
    private val _vencidos = MutableStateFlow<List<ProductoVencidoDTO>>(emptyList())
    val vencidos: StateFlow<List<ProductoVencidoDTO>> = _vencidos

    private val _inmoviles = MutableStateFlow<List<ProductoInmovilDTO>>(emptyList())
    val inmoviles: StateFlow<List<ProductoInmovilDTO>> = _inmoviles

    private val _inconsistencias = MutableStateFlow<List<InconsistenciaStockDTO>>(emptyList())
    val inconsistencias: StateFlow<List<InconsistenciaStockDTO>> = _inconsistencias

    private val _resumen = MutableStateFlow<ResumenPerdidasDTO?>(null)
    val resumen: StateFlow<ResumenPerdidasDTO?> = _resumen

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarTodo() }

    fun cargarTodo() {
        viewModelScope.launch {
            try {
                _vencidos.value = perdidasRepository.getProductosVencidos().getOrDefault(emptyList())
                _inmoviles.value = perdidasRepository.getProductosInmoviles().getOrDefault(emptyList())
                _inconsistencias.value = perdidasRepository.getInconsistenciasStock().getOrDefault(emptyList())
                _resumen.value = perdidasRepository.getResumenPerdidas().getOrNull()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}