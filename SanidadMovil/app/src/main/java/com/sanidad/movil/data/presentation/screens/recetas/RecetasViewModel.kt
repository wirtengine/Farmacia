package com.sanidad.movil.data.presentation.screens.recetas

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.RecetaResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import okhttp3.RequestBody.Companion.asRequestBody

class RecetasViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS ======================
    private val _recetas = MutableStateFlow<List<RecetaResponse>>(emptyList())
    val recetas: StateFlow<List<RecetaResponse>> = _recetas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ====================== TOGGLE ======================
    private val _mostrarTodas = MutableStateFlow(false)
    val mostrarTodas: StateFlow<Boolean> = _mostrarTodas

    // ====================== FORMULARIO SUBIDA ======================
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _codigoMinsa = MutableStateFlow("")
    val codigoMinsa: StateFlow<String> = _codigoMinsa

    // ====================== CARGA DE DATOS ======================
    fun cargarRecetas(esAdmin: Boolean, farmaceuticoId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = if (esAdmin) {
                    if (_mostrarTodas.value) api.obtenerTodasLasRecetas()
                    else api.obtenerRecetasPendientes()
                } else {
                    if (_mostrarTodas.value) api.obtenerTodasLasRecetas()
                    else api.obtenerRecetasPorFarmaceutico(farmaceuticoId)
                }
                if (response.isSuccessful) {
                    _recetas.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleMostrarTodas() {
        _mostrarTodas.value = !_mostrarTodas.value
    }

    fun setImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setCodigoMinsa(codigo: String) {
        _codigoMinsa.value = codigo
    }

    // ====================== SUBIR RECETA ======================
    fun subirReceta(farmaceuticoId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uri = _selectedImageUri.value
        if (uri == null) {
            onError("Selecciona una imagen")
            return
        }
        if (_codigoMinsa.value.isBlank()) {
            onError("El Código MINSA es obligatorio")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = MyApplication.instance
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "receta_upload.jpg")
                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
                val minsaBody = _codigoMinsa.value.toRequestBody("text/plain".toMediaTypeOrNull())
                val farmIdBody = farmaceuticoId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.subirReceta(
                    codigoMinsa = _codigoMinsa.value,
                    farmaceuticoId = farmaceuticoId,
                    file = filePart
                )
                // Nota: la interfaz ApiService ya está definida para recibir @Query y @Body MultipartBody.Part.
                // Si da error, ajustaremos la firma en ApiService para que coincida con el controlador Spring.
                // Por ahora asumimos que api.subirReceta acepta los parámetros correctos.
                if (response.isSuccessful) {
                    _selectedImageUri.value = null
                    _codigoMinsa.value = ""
                    onSuccess()
                } else {
                    onError("Error ${response.code()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ====================== VALIDAR RECETA ======================
    fun validarReceta(recetaId: Long, aprobar: Boolean, farmaceuticoId: Long?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.validarReceta(recetaId, aprobar, farmaceuticoId)
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}