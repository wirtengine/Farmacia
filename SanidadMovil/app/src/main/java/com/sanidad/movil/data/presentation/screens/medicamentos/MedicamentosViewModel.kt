package com.sanidad.movil.presentation.screens.medicamentos

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.MedicamentoRequest
import com.sanidad.movil.data.remote.dto.MedicamentoResponse
import com.sanidad.movil.data.repository.MedicamentoRepository
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
import java.text.NumberFormat
import java.util.*

data class MedicamentoFormState(
    val registroSanitario: String = "",
    val nombre: String = "",
    val presentacion: String = "Tableta",
    val via: String = "ORAL",
    val fabricante: String = "",
    val tipoVenta: String = "LIBRE",
    val precioUnitario: String = "",
    val receta: Boolean = false
)

data class MedicamentosUiState(
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val showSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val editingId: Long? = null,
    val formData: MedicamentoFormState = MedicamentoFormState(),
    val formError: String? = null,
    val imageUri: android.net.Uri? = null,
    val showConfirmDialog: Boolean = false,
    val confirmTitle: String = "",
    val confirmText: String = "",
    val confirmAction: ConfirmAction? = null
)

enum class ConfirmAction {
    DESACTIVAR, REACTIVAR
}

class MedicamentosViewModel(
    private val medicamentoRepo: MedicamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicamentosUiState())
    val uiState: StateFlow<MedicamentosUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarMedicamentos() }

    fun cargarMedicamentos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = medicamentoRepo.getMedicamentos()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.id }
                    val filtered = filtrarMedicamentos(sorted, _uiState.value.searchQuery)
                    _uiState.value = _uiState.value.copy(
                        medicamentos = sorted,
                        isLoading = false,
                        totalPages = calcularTotalPaginas(filtered.size),
                        currentPage = 1
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        medicamentos = emptyList(),
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

    fun abrirNuevo() {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = false,
            editingId = null,
            formData = MedicamentoFormState(),
            formError = null,
            imageUri = null
        )
    }

    fun abrirEdicion(med: MedicamentoResponse) {
        _uiState.value = _uiState.value.copy(
            showSheet = true,
            isEditMode = true,
            editingId = med.id,
            formData = MedicamentoFormState(
                registroSanitario = med.registroSanitario,
                nombre = med.nombre,
                presentacion = med.presentacion ?: "Tableta",
                via = med.via ?: "ORAL",
                fabricante = med.fabricante ?: "",
                tipoVenta = med.tipoVenta ?: "LIBRE",
                precioUnitario = med.precioUnitario.toString(),
                receta = med.receta ?: false
            ),
            formError = null,
            imageUri = null
        )
    }

    fun cerrarSheet() {
        _uiState.value = _uiState.value.copy(showSheet = false, formError = null)
    }

    fun actualizarCampo(campo: String, valor: Any) {
        val form = _uiState.value.formData
        val nuevo = when (campo) {
            "registroSanitario" -> form.copy(registroSanitario = valor as String)
            "nombre" -> form.copy(nombre = valor as String)
            "presentacion" -> form.copy(presentacion = valor as String)
            "via" -> form.copy(via = valor as String)
            "fabricante" -> form.copy(fabricante = valor as String)
            "tipoVenta" -> form.copy(tipoVenta = valor as String)
            "precioUnitario" -> form.copy(precioUnitario = valor as String)
            "receta" -> form.copy(receta = valor as Boolean)
            else -> form
        }
        _uiState.value = _uiState.value.copy(formData = nuevo, formError = null)
    }

    fun setImageUri(uri: android.net.Uri?) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
    }

    fun guardarMedicamento() {
        val form = _uiState.value.formData
        if (form.nombre.isBlank() || form.registroSanitario.isBlank() || form.fabricante.isBlank()) {
            _uiState.value = _uiState.value.copy(formError = "Complete los campos obligatorios")
            return
        }
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = if (_uiState.value.isEditMode) {
                medicamentoRepo.actualizarMedicamento(_uiState.value.editingId!!, request)
            } else {
                medicamentoRepo.crearMedicamento(request)
            }
            when (result) {
                is ApiResult.Success -> {
                    val targetId = result.data.id
                    _uiState.value.imageUri?.let { uri ->
                        try {
                            val file = convertirUriAFile(uri)
                            if (file != null) {
                                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
                                medicamentoRepo.subirImagen(targetId, part)
                            }
                        } catch (_: Exception) {}
                    }
                    _uiState.value = _uiState.value.copy(showSheet = false, isLoading = false)
                    cargarMedicamentos()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, formError = result.message)
                }
                else -> {}
            }
        }
    }

    private suspend fun convertirUriAFile(uri: android.net.Uri): File? = withContext(Dispatchers.IO) {
        try {
            val context = MyApplication.instance
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val file = File(context.cacheDir, "upload_temp.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun solicitarDesactivar(id: Long) {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = true,
            confirmTitle = "¿Desactivar medicamento?",
            confirmText = "El medicamento dejará de estar disponible.",
            confirmAction = ConfirmAction.DESACTIVAR,
            editingId = id
        )
    }

    fun solicitarReactivar(id: Long) {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = true,
            confirmTitle = "¿Reactivar medicamento?",
            confirmText = "Volverá a estar disponible.",
            confirmAction = ConfirmAction.REACTIVAR,
            editingId = id
        )
    }

    fun cancelarConfirmacion() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun confirmarAccion() {
        val id = _uiState.value.editingId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = when (_uiState.value.confirmAction) {
                ConfirmAction.DESACTIVAR -> medicamentoRepo.desactivarMedicamento(id)
                ConfirmAction.REACTIVAR -> medicamentoRepo.activarMedicamento(id)
                else -> null
            }
            if (result != null) {
                when (result) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(showConfirmDialog = false, isLoading = false)
                        cargarMedicamentos()
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
    }

    fun generarPDF() {
        viewModelScope.launch {
            try {
                val file = generarPDFConDatos(_uiState.value.medicamentos)
                _uiState.value = _uiState.value.copy(error = "PDF generado en ${file.absolutePath}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al generar PDF: ${e.message}")
            }
        }
    }

    private suspend fun generarPDFConDatos(medicamentos: List<MedicamentoResponse>): File = withContext(Dispatchers.IO) {
        val context = MyApplication.instance
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var currentPage = pdf.startPage(pageInfo)
        var canvas = currentPage.canvas

        val paintTitle = Paint().apply {
            color = android.graphics.Color.rgb(41, 128, 185)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintText = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val paintHeader = Paint().apply {
            color = android.graphics.Color.rgb(100, 100, 100)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintLine = Paint().apply {
            color = android.graphics.Color.rgb(200, 200, 200)
            strokeWidth = 1f
        }

        var y = margin

        // Cabecera primera página
        canvas.drawText("Catálogo de Medicamentos - Sanidad App", margin.toFloat(), y.toFloat(), paintTitle)
        y += 25
        val fechaStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Generado: $fechaStr", margin.toFloat(), y.toFloat(), paintText)
        y += 20
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 20

        canvas.drawText("Medicamento", margin.toFloat(), y.toFloat(), paintHeader)
        canvas.drawText("Reg. Sanitario", 200f, y.toFloat(), paintHeader)
        canvas.drawText("Fabricante", 300f, y.toFloat(), paintHeader)
        canvas.drawText("Precio", 450f, y.toFloat(), paintHeader)
        canvas.drawText("Estado", 510f, y.toFloat(), paintHeader)
        y += 10
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 15

        for (m in medicamentos) {
            canvas.drawText(m.nombre, margin.toFloat(), y.toFloat(), paintText)
            canvas.drawText(m.registroSanitario, 200f, y.toFloat(), paintText)
            canvas.drawText(m.fabricante ?: "N/A", 300f, y.toFloat(), paintText)
            canvas.drawText("C$ ${"%.2f".format(m.precioUnitario)}", 450f, y.toFloat(), paintText)
            canvas.drawText(if (m.activo) "Activo" else "Inactivo", 510f, y.toFloat(), paintText)
            y += 20

            if (y > pageHeight - margin) {
                pdf.finishPage(currentPage)
                currentPage = pdf.startPage(pageInfo)
                canvas = currentPage.canvas
                y = margin
                // repetir cabecera en nueva página
                canvas.drawText("Catálogo de Medicamentos - Sanidad App (continuación)", margin.toFloat(), y.toFloat(), paintTitle)
                y += 25
                canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
                y += 20
                canvas.drawText("Medicamento", margin.toFloat(), y.toFloat(), paintHeader)
                canvas.drawText("Reg. Sanitario", 200f, y.toFloat(), paintHeader)
                canvas.drawText("Fabricante", 300f, y.toFloat(), paintHeader)
                canvas.drawText("Precio", 450f, y.toFloat(), paintHeader)
                canvas.drawText("Estado", 510f, y.toFloat(), paintHeader)
                y += 10
                canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
                y += 15
            }
        }
        pdf.finishPage(currentPage)

        val file = File(context.cacheDir, "Reporte_Medicamentos_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        file
    }

    private fun filtrarMedicamentos(lista: List<MedicamentoResponse>, query: String): List<MedicamentoResponse> {
        if (query.isBlank()) return lista
        val term = query.lowercase().trim()
        return lista.filter {
            it.nombre.lowercase().contains(term) ||
                    it.fabricante?.lowercase()?.contains(term) == true ||
                    it.registroSanitario.lowercase().contains(term)
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val medicamentosFiltrados: List<MedicamentoResponse>
        get() = filtrarMedicamentos(_uiState.value.medicamentos, _uiState.value.searchQuery)

    val paginatedMedicamentos: List<MedicamentoResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, medicamentosFiltrados.size)
            if (start >= end) return emptyList()
            return medicamentosFiltrados.subList(start, end)
        }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = medicamentosFiltrados.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}