package com.sanidad.movil.presentation.screens.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.ClienteRequest
import com.sanidad.movil.data.remote.dto.ClienteResponse
import com.sanidad.movil.data.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ClienteFormState(
    val cedula: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val saldo: String = "0.0"
)

data class ClientesUiState(
    val clientes: List<ClienteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val showForm: Boolean = false,
    val isEditMode: Boolean = false,
    val editingId: Long? = null,
    val formData: ClienteFormState = ClienteFormState(),
    val formError: String? = null,
    val showConfirmDeactivate: Boolean = false,
    val clienteToDeactivate: Long? = null
)

class ClientesViewModel(
    private val clienteRepository: ClienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarClientes() }

    fun cargarClientes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = clienteRepository.getClientes()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    val filtered = filtrarClientes(sorted, _uiState.value.searchTerm)
                    _uiState.value = _uiState.value.copy(
                        clientes = sorted,
                        isLoading = false,
                        totalPages = calcularTotalPaginas(filtered.size),
                        currentPage = 1
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        clientes = emptyList(),
                        totalPages = 1
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun setSearch(term: String) {
        _uiState.value = _uiState.value.copy(searchTerm = term, currentPage = 1)
        actualizarPaginacion()
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    fun abrirNuevo() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            isEditMode = false,
            editingId = null,
            formData = ClienteFormState(),
            formError = null
        )
    }

    fun abrirEdicion(cliente: ClienteResponse) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            isEditMode = true,
            editingId = cliente.id,
            formData = ClienteFormState(
                cedula = cliente.cedula,
                nombre = cliente.nombre,
                telefono = cliente.telefono ?: "",
                email = cliente.email ?: "",
                saldo = cliente.saldo.toString()
            ),
            formError = null
        )
    }

    fun cerrarFormulario() {
        _uiState.value = _uiState.value.copy(showForm = false, formError = null)
    }

    fun actualizarCampo(campo: String, valor: String) {
        val current = _uiState.value.formData
        val newForm = when (campo) {
            "cedula" -> current.copy(cedula = valor)
            "nombre" -> current.copy(nombre = valor)
            "telefono" -> current.copy(telefono = valor)
            "email" -> current.copy(email = valor)
            "saldo" -> current.copy(saldo = valor)
            else -> current
        }
        _uiState.value = _uiState.value.copy(formData = newForm, formError = null)
    }

    fun guardarCliente() {
        val form = _uiState.value.formData
        if (form.cedula.isBlank() || form.nombre.isBlank()) {
            _uiState.value = _uiState.value.copy(formError = "Cédula y Nombre son obligatorios")
            return
        }
        val request = ClienteRequest(
            cedula = form.cedula.trim(),
            nombre = form.nombre.trim(),
            telefono = form.telefono.trim().ifBlank { null },
            email = form.email.trim().ifBlank { null },
            saldo = form.saldo.toDoubleOrNull() ?: 0.0
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = if (_uiState.value.isEditMode) {
                clienteRepository.actualizarCliente(_uiState.value.editingId!!, request)
            } else {
                clienteRepository.crearCliente(request)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showForm = false, isLoading = false)
                    cargarClientes()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        formError = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun solicitarDesactivar(id: Long) {
        _uiState.value = _uiState.value.copy(showConfirmDeactivate = true, clienteToDeactivate = id)
    }

    fun cancelarDesactivar() {
        _uiState.value = _uiState.value.copy(showConfirmDeactivate = false, clienteToDeactivate = null)
    }

    fun confirmarDesactivar() {
        val id = _uiState.value.clienteToDeactivate ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = clienteRepository.suspenderCliente(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showConfirmDeactivate = false, clienteToDeactivate = null)
                    cargarClientes()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        showConfirmDeactivate = false
                    )
                }
                else -> {}
            }
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ---------- Helpers ----------
    private fun filtrarClientes(lista: List<ClienteResponse>, term: String): List<ClienteResponse> {
        if (term.isBlank()) return lista
        val lower = term.lowercase().trim()
        return lista.filter { it.nombre.lowercase().contains(lower) || it.cedula.contains(lower) }
    }

    private fun calcularTotalPaginas(total: Int) =
        if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    // Propiedades calculadas para la UI
    val clientesFiltrados: List<ClienteResponse>
        get() = filtrarClientes(_uiState.value.clientes, _uiState.value.searchTerm)

    val paginatedClientes: List<ClienteResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, clientesFiltrados.size)
            if (start >= end) return emptyList()
            return clientesFiltrados.subList(start, end)
        }

    private fun actualizarPaginacion() {
        val total = clientesFiltrados.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}