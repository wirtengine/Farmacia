package com.sanidad.movil.presentation.screens.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.VentaResponse
import com.sanidad.movil.data.repository.VentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VentasUiState(
    val ventas: List<VentaResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val empleadoFiltrado: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

class VentasViewModel(
    private val ventaRepository: VentaRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VentasUiState(isLoading = true))
    val uiState: StateFlow<VentasUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarVentas() }

    fun cargarVentas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = ventaRepository.getVentas()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    _uiState.value = _uiState.value.copy(
                        ventas = sorted,
                        isLoading = false,
                        totalPages = calcularPaginas(sorted.size),
                        currentPage = 1
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        ventas = emptyList()
                    )
                }
                else -> {}
            }
        }
    }

    fun setSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, currentPage = 1)
    }

    fun setEmpleadoFiltrado(empleado: String?) {
        _uiState.value = _uiState.value.copy(empleadoFiltrado = empleado, currentPage = 1)
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    val ventasFiltradas: List<VentaResponse>
        get() {
            var lista = _uiState.value.ventas
            val q = _uiState.value.searchQuery.lowercase().trim()
            if (q.isNotEmpty()) {
                lista = lista.filter {
                    it.numeroFactura.lowercase().contains(q) ||
                            it.clienteNombre?.lowercase()?.contains(q) == true
                }
            }
            _uiState.value.empleadoFiltrado?.let { emp ->
                lista = lista.filter { it.usuarioUsername == emp }
            }
            return lista
        }

    val paginatedVentas: List<VentaResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, ventasFiltradas.size)
            return if (start >= end) emptyList() else ventasFiltradas.subList(start, end)
        }

    val empleados: List<String>
        get() = _uiState.value.ventas.map { it.usuarioUsername }.distinct()

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun calcularPaginas(total: Int) =
        if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
}