package com.sanidad.movil.data.presentation.screens.proveedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.ProveedorRequest
import com.sanidad.movil.data.remote.dto.ProveedorResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProveedoresViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    private val _proveedores = MutableStateFlow<List<ProveedorResponse>>(emptyList())
    val proveedores: StateFlow<List<ProveedorResponse>> = _proveedores

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage
    val rowsPerPage = 15

    // Formulario
    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId

    private val _formData = MutableStateFlow(ProveedorFormState())
    val formData: StateFlow<ProveedorFormState> = _formData

    fun cargarProveedores() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.obtenerProveedores()
                if (response.isSuccessful) {
                    _proveedores.value = response.body()?.sortedByDescending { it.id } ?: emptyList()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Filtrado: solo activos y búsqueda
    val proveedoresFiltrados: List<ProveedorResponse>
        get() {
            val query = _searchQuery.value.lowercase().trim()
            return _proveedores.value.filter { p ->
                p.activo && (query.isEmpty() ||
                        p.nombre.lowercase().contains(query) ||
                        p.ruc.lowercase().contains(query))
            }
        }

    val totalPages: Int
        get() {
            val total = proveedoresFiltrados.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedProveedores: List<ProveedorResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, proveedoresFiltrados.size)
            if (start >= end) return emptyList()
            return proveedoresFiltrados.subList(start, end)
        }

    fun setSearch(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        if (page in 1..totalPages) _currentPage.value = page
    }

    // Abrir formulario para nuevo
    fun abrirNuevo() {
        _isEditMode.value = false
        _editingId.value = null
        _formData.value = ProveedorFormState()
        _showSheet.value = true
    }

    // Abrir formulario para editar
    fun abrirEdicion(prov: ProveedorResponse) {
        _isEditMode.value = true
        _editingId.value = prov.id
        _formData.value = ProveedorFormState(
            ruc = prov.ruc,
            nombre = prov.nombre,
            telefono = prov.telefono ?: "",
            email = prov.email ?: ""
        )
        _showSheet.value = true
    }

    fun cerrarSheet() {
        _showSheet.value = false
    }

    fun updateCampo(campo: String, valor: String) {
        _formData.value = when (campo) {
            "ruc" -> _formData.value.copy(ruc = valor)
            "nombre" -> _formData.value.copy(nombre = valor)
            "telefono" -> _formData.value.copy(telefono = valor)
            "email" -> _formData.value.copy(email = valor)
            else -> _formData.value
        }
    }

    fun guardarProveedor(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val form = _formData.value
            if (form.ruc.isBlank() || form.nombre.isBlank()) {
                onError("RUC y Nombre son obligatorios")
                return@launch
            }
            val request = ProveedorRequest(
                ruc = form.ruc.trim(),
                nombre = form.nombre.trim(),
                telefono = form.telefono.trim().ifBlank { null },
                email = form.email.trim().ifBlank { null }
            )
            try {
                val response = if (_isEditMode.value && _editingId.value != null) {
                    api.actualizarProveedor(_editingId.value!!, request)
                } else {
                    api.crearProveedor(request)
                }
                if (response.isSuccessful) {
                    onSuccess()
                    cargarProveedores()
                } else {
                    onError("Error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    fun desactivarProveedor(id: Long, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                api.suspenderProveedor(id)
                onResult()
                cargarProveedores()
            } catch (_: Exception) { }
        }
    }
}

data class ProveedorFormState(
    val ruc: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = ""
)