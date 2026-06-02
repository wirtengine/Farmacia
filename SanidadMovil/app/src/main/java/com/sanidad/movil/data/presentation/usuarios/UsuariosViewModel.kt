package com.sanidad.movil.data.presentation.screens.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.ActualizarUsuarioRequest
import com.sanidad.movil.data.remote.dto.UsuarioRequest
import com.sanidad.movil.data.remote.dto.UsuarioResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UsuariosViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS ======================
    private val _usuarios = MutableStateFlow<List<UsuarioResponse>>(emptyList())
    val usuarios: StateFlow<List<UsuarioResponse>> = _usuarios

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

    private val _formData = MutableStateFlow(UsuarioFormState())
    val formData: StateFlow<UsuarioFormState> = _formData

    // ====================== FILTRADO Y PAGINACIÓN ======================
    val usuariosFiltrados: List<UsuarioResponse>
        get() {
            val term = _searchTerm.value.lowercase().trim()
            return _usuarios.value.filter { u ->
                term.isEmpty() || u.username.lowercase().contains(term) || u.rol.lowercase().contains(term)
            }
        }

    val totalPages: Int
        get() {
            val total = usuariosFiltrados.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedUsuarios: List<UsuarioResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, usuariosFiltrados.size)
            if (start >= end) return emptyList()
            return usuariosFiltrados.subList(start, end)
        }

    // ====================== CARGA INICIAL ======================
    fun cargarUsuarios() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.obtenerUsuarios()
                if (response.isSuccessful) {
                    _usuarios.value = response.body()?.sortedByDescending { it.id } ?: emptyList()
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
        _formData.value = UsuarioFormState()
        _showSheet.value = true
    }

    fun abrirEdicion(usuario: UsuarioResponse) {
        _isEditMode.value = true
        _editingId.value = usuario.id
        _formData.value = UsuarioFormState(
            username = usuario.username,
            password = "",
            rol = usuario.rol
        )
        _showSheet.value = true
    }

    fun cerrarSheet() {
        _showSheet.value = false
    }

    fun updateCampo(campo: String, valor: String) {
        _formData.value = when (campo) {
            "username" -> _formData.value.copy(username = valor)
            "password" -> _formData.value.copy(password = valor)
            "rol" -> _formData.value.copy(rol = valor)
            else -> _formData.value
        }
    }

    // ====================== GUARDAR ======================
    fun guardarUsuario(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val form = _formData.value
            if (!_isEditMode.value && (form.username.isBlank() || form.password.isBlank())) {
                onError("Usuario y contraseña son obligatorios")
                return@launch
            }
            try {
                if (_isEditMode.value) {
                    val request = ActualizarUsuarioRequest(
                        password = form.password.ifBlank { null },
                        rol = form.rol
                    )
                    val response = api.actualizarUsuario(_editingId.value!!, request)
                    if (response.isSuccessful) {
                        onSuccess()
                        cargarUsuarios()
                    } else {
                        onError("Error ${response.code()}")
                    }
                } else {
                    val request = UsuarioRequest(
                        username = form.username.trim(),
                        password = form.password,
                        rol = form.rol
                    )
                    val response = api.crearUsuario(request)
                    if (response.isSuccessful) {
                        onSuccess()
                        cargarUsuarios()
                    } else {
                        onError("Error ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }
}

data class UsuarioFormState(
    val username: String = "",
    val password: String = "",
    val rol: String = "VENDEDOR"
)