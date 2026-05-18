package com.sanidad.movil.data.presentation.screens.medicamentos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.MedicamentoRequest
import com.sanidad.movil.data.remote.dto.MedicamentoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MedicamentosViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // -------------------- Lista --------------------
    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // -------------------- Búsqueda y paginación --------------------
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    val rowsPerPage = 15

    // -------------------- Medicamento filtrado paginado --------------------
    val medicamentosFiltrados: List<MedicamentoResponse>
        get() {
            val query = _searchQuery.value.lowercase()
            return _medicamentos.value
                .filter { m ->
                    m.nombre.lowercase().contains(query) ||
                            m.fabricante.lowercase().contains(query) ||
                            m.registroSanitario.lowercase().contains(query)
                }
        }

    val totalPages: Int
        get() {
            val total = medicamentosFiltrados.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedMedicamentos: List<MedicamentoResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, medicamentosFiltrados.size)
            return medicamentosFiltrados.subList(
                start.coerceAtMost(end),
                end
            )
        }

    // -------------------- Formulario --------------------
    private val _formData = MutableStateFlow(FormState())
    val formData: StateFlow<FormState> = _formData

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri

    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet

    fun cargarMedicamentos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.obtenerMedicamentos()
                if (response.isSuccessful) {
                    _medicamentos.value = response.body()?.sortedByDescending { it.id } ?: emptyList()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
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
        _formData.value = FormState()
        _imageUri.value = null
        _showSheet.value = true
    }

    // Abrir formulario para editar
    fun abrirEdicion(med: MedicamentoResponse) {
        _isEditMode.value = true
        _editingId.value = med.id
        _formData.value = FormState(
            registroSanitario = med.registroSanitario,
            nombre = med.nombre,
            presentacion = med.presentacion,
            via = med.via,
            fabricante = med.fabricante,
            tipoVenta = med.tipoVenta,
            precioUnitario = med.precioUnitario.toString(),
            receta = med.receta
        )
        _imageUri.value = null
        _showSheet.value = true
    }

    fun actualizarCampo(campo: String, valor: Any) {
        _formData.value = _formData.value.copy().also {
            when (campo) {
                "registroSanitario" -> it.registroSanitario = valor as String
                "nombre" -> it.nombre = valor as String
                "presentacion" -> it.presentacion = valor as String
                "via" -> it.via = valor as String
                "fabricante" -> it.fabricante = valor as String
                "tipoVenta" -> it.tipoVenta = valor as String
                "precioUnitario" -> it.precioUnitario = valor as String
                "receta" -> it.receta = valor as Boolean
            }
        }
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun cerrarSheet() {
        _showSheet.value = false
    }

    // Guardar (crear o actualizar)
    fun guardarMedicamento(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val form = _formData.value
            val request = MedicamentoRequest(
                registroSanitario = form.registroSanitario,
                nombre = form.nombre,
                presentacion = form.presentacion,
                via = form.via,
                fabricante = form.fabricante,
                tipoVenta = form.tipoVenta,
                precioUnitario = form.precioUnitario.toDoubleOrNull() ?: 0.0,
                receta = form.receta
            )

            try {
                if (_isEditMode.value) {
                    val id = _editingId.value!!
                    api.actualizarMedicamento(id, request)
                } else {
                    api.crearMedicamento(request)
                }.let { response ->
                    if (response.isSuccessful) {
                        val targetId = response.body()?.id ?: _editingId.value!!
                        // Subir imagen si hay
                        _imageUri.value?.let { uri ->
                            try {
                                subirImagen(targetId, uri)
                            } catch (_: Exception) {
                                onError("Guardado, pero falló la imagen")
                            }
                        }
                        onSuccess()
                        cargarMedicamentos()
                    } else {
                        onError("Error ${response.code()}: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    private suspend fun subirImagen(medicamentoId: Long, uri: Uri) {
        // Leer el archivo desde el URI
        val context = com.sanidad.movil.MyApplication.instance
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_temp.jpg")
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        api.subirImagenMedicamento(medicamentoId, part)
    }

    // Desactivar / Reactivar
    fun desactivarMedicamento(id: Long, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                api.desactivarMedicamento(id)
                onResult()
                cargarMedicamentos()
            } catch (_: Exception) {}
        }
    }

    fun reactivarMedicamento(id: Long, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                api.activarMedicamento(id)
                onResult()
                cargarMedicamentos()
            } catch (_: Exception) {}
        }
    }
}

data class FormState(
    var registroSanitario: String = "",
    var nombre: String = "",
    var presentacion: String = "Tableta",
    var via: String = "ORAL",
    var fabricante: String = "",
    var tipoVenta: String = "LIBRE",
    var precioUnitario: String = "",
    var receta: Boolean = false
)