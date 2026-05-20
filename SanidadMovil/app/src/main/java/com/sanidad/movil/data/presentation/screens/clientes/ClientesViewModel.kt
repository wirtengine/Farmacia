package com.sanidad.movil.data.presentation.screens.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.ClienteRequest
import com.sanidad.movil.data.remote.dto.ClienteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientesViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS ======================
    private val _clientes = MutableStateFlow<List<ClienteResponse>>(emptyList())
    val clientes: StateFlow<List<ClienteResponse>> = _clientes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ====================== BÚSQUEDA Y PAGINACIÓN ======================
    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage
    val rowsPerPage = 15

    // ====================== FORMULARIO ======================
    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId

    private val _formData = MutableStateFlow(ClienteFormState())
    val formData: StateFlow<ClienteFormState> = _formData

    // ====================== FILTRADO Y PAGINACIÓN ======================
    val clientesFiltrados: List<ClienteResponse>
        get() {
            val term = _searchTerm.value.lowercase().trim()
            return _clientes.value.filter { c ->
                term.isEmpty() || c.nombre.lowercase().contains(term) || c.cedula.contains(term)
            }
        }

    val totalPages: Int
        get() {
            val total = clientesFiltrados.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedClientes: List<ClienteResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, clientesFiltrados.size)
            if (start >= end) return emptyList()
            return clientesFiltrados.subList(start, end)
        }

    // ====================== CARGA INICIAL ======================
    fun cargarClientes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.obtenerClientes()
                if (response.isSuccessful) {
                    _clientes.value = response.body()?.sortedByDescending { it.id } ?: emptyList()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearch(term: String) {
        _searchTerm.value = term
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        if (page in 1..totalPages) _currentPage.value = page
    }

    // ====================== ABRIR FORMULARIO ======================
    fun abrirNuevo() {
        _isEditMode.value = false
        _editingId.value = null
        _formData.value = ClienteFormState()
        _showSheet.value = true
    }

    fun abrirEdicion(cliente: ClienteResponse) {
        _isEditMode.value = true
        _editingId.value = cliente.id
        _formData.value = ClienteFormState(
            cedula = cliente.cedula,
            nombre = cliente.nombre,
            telefono = cliente.telefono ?: "",
            email = cliente.email ?: "",
            saldo = cliente.saldo.toString()
        )
        _showSheet.value = true
    }

    fun cerrarSheet() {
        _showSheet.value = false
    }

    fun updateCampo(campo: String, valor: Any) {
        _formData.value = when (campo) {
            "cedula" -> _formData.value.copy(cedula = valor as String)
            "nombre" -> _formData.value.copy(nombre = valor as String)
            "telefono" -> _formData.value.copy(telefono = valor as String)
            "email" -> _formData.value.copy(email = valor as String)
            "saldo" -> _formData.value.copy(saldo = valor as String)
            else -> _formData.value
        }
    }

    // ====================== GUARDAR ======================
    fun guardarCliente(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val form = _formData.value
            if (form.cedula.isBlank() || form.nombre.isBlank()) {
                onError("Cédula y Nombre son obligatorios")
                return@launch
            }
            val request = ClienteRequest(
                cedula = form.cedula.trim(),
                nombre = form.nombre.trim(),
                telefono = form.telefono.trim().ifBlank { null },
                email = form.email.trim().ifBlank { null },
                saldo = form.saldo.toDoubleOrNull() ?: 0.0
            )
            try {
                val response = if (_isEditMode.value) {
                    api.actualizarCliente(_editingId.value!!, request)
                } else {
                    api.crearCliente(request)
                }
                if (response.isSuccessful) {
                    onSuccess()
                    cargarClientes()
                } else {
                    onError("Error ${response.code()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    // ====================== DESACTIVAR ======================
    fun desactivarCliente(id: Long, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                api.suspenderCliente(id)
                onResult()
                cargarClientes()
            } catch (_: Exception) {
            }
        }
    }
}

data class ClienteFormState(
    val cedula: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val saldo: String = "0.0"
)