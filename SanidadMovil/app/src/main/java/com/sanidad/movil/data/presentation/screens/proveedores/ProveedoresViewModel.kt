package com.sanidad.movil.presentation.screens.proveedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.ProveedorRequest
import com.sanidad.movil.data.remote.dto.ProveedorResponse
import com.sanidad.movil.data.repository.ProveedorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProveedorFormState(
    val ruc: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = ""
)

data class ProveedoresUiState(
    val proveedores: List<ProveedorResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    // Formulario
    val showSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val editingId: Long? = null,
    val formData: ProveedorFormState = ProveedorFormState(),
    val formError: String? = null,

    // Diálogo de confirmación
    val showConfirmDialog: Boolean = false,
    val proveedorToDelete: Long? = null
)

class ProveedoresViewModel(
    private val proveedorRepo: ProveedorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProveedoresUiState())
    val uiState: StateFlow<ProveedoresUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarProveedores() }

    fun cargarProveedores() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = proveedorRepo.getProveedores()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    val filtered = filtrarProveedores(sorted, _uiState.value.searchQuery)
                    _uiState.value = _uiState.value.copy(
                        proveedores = sorted,
                        isLoading = false,
                        totalPages = calcularTotalPaginas(filtered.size),
                        currentPage = 1
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        proveedores = emptyList(),
                        totalPages = 1
                    )
                }
                else -> {}
            }
        }
    }

    fun setSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, currentPage = 1)
        actualizarPaginacion()
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    // ---- Formulario ----
    fun abrirNuevo() {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = false,
            editingId = null,
            formData = ProveedorFormState(),
            formError = null
        )
    }

    fun abrirEdicion(prov: ProveedorResponse) {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = true,
            editingId = prov.id,
            formData = ProveedorFormState(
                ruc = prov.ruc,
                nombre = prov.nombre,
                telefono = prov.telefono ?: "",
                email = prov.email ?: ""
            ),
            formError = null
        )
    }

    fun cerrarSheet() {
        _uiState.value = _uiState.value.copy(showSheet = false, formError = null)
    }

    fun updateCampo(campo: String, valor: String) {
        val form = _uiState.value.formData
        val nuevo = when (campo) {
            "ruc" -> form.copy(ruc = valor)
            "nombre" -> form.copy(nombre = valor)
            "telefono" -> form.copy(telefono = valor)
            "email" -> form.copy(email = valor)
            else -> form
        }
        _uiState.value = _uiState.value.copy(formData = nuevo, formError = null)
    }

    fun guardarProveedor() {
        val form = _uiState.value.formData
        if (form.ruc.isBlank() || form.nombre.isBlank()) {
            _uiState.value = _uiState.value.copy(formError = "RUC y Nombre son obligatorios")
            return
        }
        val request = ProveedorRequest(
            ruc = form.ruc.trim(),
            nombre = form.nombre.trim(),
            telefono = form.telefono.trim().ifBlank { null },
            email = form.email.trim().ifBlank { null }
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = if (_uiState.value.isEditMode && _uiState.value.editingId != null) {
                proveedorRepo.actualizarProveedor(_uiState.value.editingId!!, request)
            } else {
                proveedorRepo.crearProveedor(request)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showSheet = false, isLoading = false)
                    cargarProveedores()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, formError = result.message)
                }
                else -> {}
            }
        }
    }

    // ---- Desactivar ----
    fun solicitarDesactivar(id: Long) {
        _uiState.value = _uiState.value.copy(showConfirmDialog = true, proveedorToDelete = id)
    }

    fun cancelarDesactivar() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false, proveedorToDelete = null)
    }

    fun confirmarDesactivar() {
        val id = _uiState.value.proveedorToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = proveedorRepo.suspenderProveedor(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showConfirmDialog = false,
                        proveedorToDelete = null,
                        isLoading = false
                    )
                    cargarProveedores()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        showConfirmDialog = false
                    )
                }
                else -> {}
            }
        }
    }

    // ---- Helpers ----
    private fun filtrarProveedores(lista: List<ProveedorResponse>, query: String): List<ProveedorResponse> {
        if (query.isBlank()) return lista.filter { it.activo }
        val term = query.lowercase().trim()
        return lista.filter { p ->
            p.activo && (p.nombre.lowercase().contains(term) || p.ruc.lowercase().contains(term))
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val proveedoresFiltrados: List<ProveedorResponse>
        get() = filtrarProveedores(_uiState.value.proveedores, _uiState.value.searchQuery)

    val paginatedProveedores: List<ProveedorResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, proveedoresFiltrados.size)
            if (start >= end) return emptyList()
            return proveedoresFiltrados.subList(start, end)
        }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = proveedoresFiltrados.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}