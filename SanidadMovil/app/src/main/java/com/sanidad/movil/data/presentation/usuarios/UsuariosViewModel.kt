package com.sanidad.movil.presentation.screens.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.ActualizarUsuarioRequest
import com.sanidad.movil.data.remote.dto.UsuarioRequest
import com.sanidad.movil.data.remote.dto.UsuarioResponse
import com.sanidad.movil.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UsuarioFormState(
    val username: String = "",
    val password: String = "",
    val rol: String = "VENDEDOR"
)

data class UsuariosUiState(
    val usuarios: List<UsuarioResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    // Formulario
    val showSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val editingId: Long? = null,
    val formData: UsuarioFormState = UsuarioFormState(),
    val formError: String? = null
)

class UsuariosViewModel(
    private val usuarioRepo: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsuariosUiState())
    val uiState: StateFlow<UsuariosUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarUsuarios() }

    fun cargarUsuarios() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = usuarioRepo.getUsuarios()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    val filtered = filtrarUsuarios(sorted, _uiState.value.searchTerm)
                    _uiState.value = _uiState.value.copy(
                        usuarios = sorted,
                        isLoading = false,
                        totalPages = calcularTotalPaginas(filtered.size),
                        currentPage = 1
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        usuarios = emptyList(),
                        totalPages = 1
                    )
                }
                else -> {}
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

    // ---- Formulario ----
    fun abrirNuevo() {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = false,
            editingId = null,
            formData = UsuarioFormState(),
            formError = null
        )
    }

    fun abrirEdicion(usuario: UsuarioResponse) {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = true,
            editingId = usuario.id,
            formData = UsuarioFormState(
                username = usuario.username,
                password = "",
                rol = usuario.rol
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
            "username" -> form.copy(username = valor)
            "password" -> form.copy(password = valor)
            "rol" -> form.copy(rol = valor)
            else -> form
        }
        _uiState.value = _uiState.value.copy(formData = nuevo, formError = null)
    }

    fun guardarUsuario() {
        val form = _uiState.value.formData
        if (!_uiState.value.isEditMode && (form.username.isBlank() || form.password.isBlank())) {
            _uiState.value = _uiState.value.copy(formError = "Usuario y contraseña son obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = if (_uiState.value.isEditMode) {
                val request = ActualizarUsuarioRequest(
                    password = form.password.ifBlank { null },
                    rol = form.rol
                )
                usuarioRepo.actualizarUsuario(_uiState.value.editingId!!, request)
            } else {
                val request = UsuarioRequest(
                    username = form.username.trim(),
                    password = form.password,
                    rol = form.rol
                )
                usuarioRepo.crearUsuario(request)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showSheet = false, isLoading = false)
                    cargarUsuarios()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, formError = result.message)
                }
                else -> {}
            }
        }
    }

    // ---- Helpers ----
    private fun filtrarUsuarios(lista: List<UsuarioResponse>, term: String): List<UsuarioResponse> {
        if (term.isBlank()) return lista
        val lower = term.lowercase().trim()
        return lista.filter {
            it.username.lowercase().contains(lower) || it.rol.lowercase().contains(lower)
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val usuariosFiltrados: List<UsuarioResponse>
        get() = filtrarUsuarios(_uiState.value.usuarios, _uiState.value.searchTerm)

    val paginatedUsuarios: List<UsuarioResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, usuariosFiltrados.size)
            if (start >= end) return emptyList()
            return usuariosFiltrados.subList(start, end)
        }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = usuariosFiltrados.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}