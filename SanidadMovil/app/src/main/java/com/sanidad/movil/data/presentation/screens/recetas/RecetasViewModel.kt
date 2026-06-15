package com.sanidad.movil.presentation.screens.recetas

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.RecetaResponse
import com.sanidad.movil.data.repository.RecetaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

data class RecetasUiState(
    val recetas: List<RecetaResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mostrarTodas: Boolean = false,
    val selectedImageUri: Uri? = null,
    val codigoMinsa: String = "",
    val uploadError: String? = null,
    val showValidarDialog: Boolean = false,
    val recetaIdToAction: Long? = null,
    val aprobarAction: Boolean = true
)

class RecetasViewModel(
    private val recetaRepo: RecetaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecetasUiState())
    val uiState: StateFlow<RecetasUiState> = _uiState

    fun cargarRecetas(esAdmin: Boolean, farmaceuticoId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = if (esAdmin) {
                if (_uiState.value.mostrarTodas) recetaRepo.getTodasLasRecetas()
                else recetaRepo.getRecetasPendientes()
            } else {
                if (_uiState.value.mostrarTodas) recetaRepo.getTodasLasRecetas()
                else recetaRepo.getRecetasPorFarmaceutico(farmaceuticoId)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        recetas = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        recetas = emptyList()
                    )
                }
                else -> {}
            }
        }
    }

    fun toggleMostrarTodas() {
        _uiState.value = _uiState.value.copy(mostrarTodas = !_uiState.value.mostrarTodas)
    }

    fun setImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri, uploadError = null)
    }

    fun setCodigoMinsa(codigo: String) {
        _uiState.value = _uiState.value.copy(codigoMinsa = codigo, uploadError = null)
    }

    fun subirReceta(farmaceuticoId: Long) {
        val uri = _uiState.value.selectedImageUri
        if (uri == null) {
            _uiState.value = _uiState.value.copy(uploadError = "Selecciona una imagen")
            return
        }
        if (_uiState.value.codigoMinsa.isBlank()) {
            _uiState.value = _uiState.value.copy(uploadError = "El Código MINSA es obligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val file = withContext(Dispatchers.IO) {
                    val context = MyApplication.instance
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "receta_upload.jpg")
                    inputStream?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }

                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                // Llamada al repositorio (ahora el método existe)
                val result = recetaRepo.subirReceta(
                    codigoMinsa = _uiState.value.codigoMinsa,
                    farmaceuticoId = farmaceuticoId,
                    file = filePart
                )

                // Manejo correcto de ApiResult con tipo explícito
                when (result) {
                    is ApiResult.Success<*> -> {
                        _uiState.value = _uiState.value.copy(
                            selectedImageUri = null,
                            codigoMinsa = "",
                            isLoading = false,
                            uploadError = null
                        )
                        // Opcional: recargar lista de recetas
                        cargarRecetas(true, farmaceuticoId) // o según el contexto
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            uploadError = result.message
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            uploadError = "Error desconocido"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadError = e.message ?: "Error de conexión"
                )
            }
        }
    }

    fun mostrarValidarDialog(recetaId: Long, aprobar: Boolean) {
        _uiState.value = _uiState.value.copy(
            showValidarDialog = true,
            recetaIdToAction = recetaId,
            aprobarAction = aprobar
        )
    }

    fun ocultarValidarDialog() {
        _uiState.value = _uiState.value.copy(showValidarDialog = false)
    }

    fun confirmarValidacion(farmaceuticoId: Long?) {
        val id = _uiState.value.recetaIdToAction ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = recetaRepo.validarReceta(id, _uiState.value.aprobarAction, farmaceuticoId)
            when (result) {
                is ApiResult.Success<*> -> {
                    _uiState.value = _uiState.value.copy(
                        showValidarDialog = false,
                        isLoading = false
                    )
                    // Recargar datos
                    farmaceuticoId?.let { cargarRecetas(true, it) }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        showValidarDialog = false
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error inesperado",
                        showValidarDialog = false
                    )
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null, uploadError = null)
    }
}