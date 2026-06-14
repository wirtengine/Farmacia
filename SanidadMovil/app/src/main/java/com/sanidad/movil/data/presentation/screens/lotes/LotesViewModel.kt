package com.sanidad.movil.presentation.screens.lotes

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.LoteRepository
import com.sanidad.movil.data.repository.MedicamentoRepository
import com.sanidad.movil.data.repository.ProveedorRepository
import com.sanidad.movil.data.repository.RackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DetalleFormState(
    val medicamentoId: Long? = null,
    val cantidad: Int = 1,
    val rackId: Long? = null,
    val nivel: Int = 0,
    val columna: Int = 0,
    val profundidadIndex: Int = 0
)

data class LoteFormState(
    val fechaFabricacion: String = "",
    val fechaVencimiento: String = "",
    val proveedorId: Long? = null,
    val factura: String = "",
    val detalles: List<DetalleFormState> = listOf(DetalleFormState())
)

data class LotesUiState(
    val lotes: List<LoteResponse> = emptyList(),
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val proveedores: List<ProveedorResponse> = emptyList(),
    val racks: List<RackResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filtroStock: String = "todos",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val showSheet: Boolean = false,
    val formData: LoteFormState = LoteFormState(),
    val formError: String? = null,

    val showConfirmDialog: Boolean = false,
    val loteToDelete: Long? = null
)

class LotesViewModel(
    private val loteRepo: LoteRepository,
    private val medicamentoRepo: MedicamentoRepository,
    private val proveedorRepo: ProveedorRepository,
    private val rackRepo: RackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LotesUiState())
    val uiState: StateFlow<LotesUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarDatos() }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val lotesResult = loteRepo.getLotes()
            val medsResult = medicamentoRepo.getMedicamentos()
            val provsResult = proveedorRepo.getProveedores()
            val racksResult = rackRepo.getRacks()

            val lotes = when (lotesResult) {
                is ApiResult.Success -> lotesResult.data.sortedByDescending { it.id }
                else -> emptyList()
            }
            val medicamentos = when (medsResult) {
                is ApiResult.Success -> medsResult.data
                else -> emptyList()
            }
            val proveedores = when (provsResult) {
                is ApiResult.Success -> provsResult.data
                else -> emptyList()
            }
            val racks = when (racksResult) {
                is ApiResult.Success -> racksResult.data
                else -> emptyList()
            }
            val filtered = filtrarLotes(lotes, _uiState.value.searchQuery, _uiState.value.filtroStock, medicamentos, proveedores)
            _uiState.value = _uiState.value.copy(
                lotes = lotes,
                medicamentos = medicamentos,
                proveedores = proveedores,
                racks = racks,
                isLoading = false,
                totalPages = calcularTotalPaginas(filtered.size),
                currentPage = 1
            )
        }
    }

    fun setSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, currentPage = 1)
        actualizarPaginacion()
    }

    fun setFiltroStock(filtro: String) {
        _uiState.value = _uiState.value.copy(filtroStock = filtro, currentPage = 1)
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
            formData = LoteFormState(
                fechaFabricacion = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                fechaVencimiento = "",
                proveedorId = null,
                factura = generarCodigoFactura(),
                detalles = mutableListOf(DetalleFormState())
            ),
            formError = null
        )
    }

    fun cerrarSheet() {
        _uiState.value = _uiState.value.copy(showSheet = false, formError = null)
    }

    private fun generarCodigoFactura(): String {
        val ahora = LocalDate.now()
        val random = (1000..9999).random()
        return "FAC-${ahora.year}${String.format("%02d", ahora.monthValue)}-$random"
    }

    fun addDetalle() {
        val form = _uiState.value.formData
        val nuevos = form.detalles.toMutableList()
        nuevos.add(DetalleFormState())
        _uiState.value = _uiState.value.copy(formData = form.copy(detalles = nuevos))
    }

    fun removeDetalle(index: Int) {
        val form = _uiState.value.formData
        val nuevos = form.detalles.toMutableList()
        if (nuevos.size > 1) {
            nuevos.removeAt(index)
            _uiState.value = _uiState.value.copy(formData = form.copy(detalles = nuevos))
        }
    }

    fun updateDetalle(index: Int, detalle: DetalleFormState) {
        val form = _uiState.value.formData
        val nuevos = form.detalles.toMutableList()
        if (index in nuevos.indices) {
            nuevos[index] = detalle
            _uiState.value = _uiState.value.copy(formData = form.copy(detalles = nuevos))
        }
    }

    fun updateCampo(campo: String, valor: Any) {
        val form = _uiState.value.formData
        val nuevo = when (campo) {
            "fechaFabricacion" -> form.copy(fechaFabricacion = valor as String)
            "fechaVencimiento" -> form.copy(fechaVencimiento = valor as String)
            "proveedorId" -> form.copy(proveedorId = valor as? Long)
            else -> form
        }
        _uiState.value = _uiState.value.copy(formData = nuevo, formError = null)
    }

    fun guardarLote() {
        val form = _uiState.value.formData
        if (form.proveedorId == null || form.fechaVencimiento.isBlank()) {
            _uiState.value = _uiState.value.copy(formError = "Complete los campos obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val request = LoteRequest(
                fechaFabricacion = form.fechaFabricacion.ifBlank { null },
                fechaVencimiento = form.fechaVencimiento,
                proveedorId = form.proveedorId,
                factura = form.factura.ifBlank { null },
                detalles = form.detalles.map { d ->
                    LoteDetalleRequest(
                        medicamentoId = d.medicamentoId!!,
                        cantidad = d.cantidad,
                        rackId = d.rackId,
                        nivel = d.nivel,
                        columna = d.columna,
                        profundidadIndex = d.profundidadIndex
                    )
                }
            )
            when (val result = loteRepo.crearLote(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showSheet = false, isLoading = false)
                    cargarDatos()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, formError = result.message)
                }
                else -> {}
            }
        }
    }

    fun solicitarDesactivar(id: Long) {
        _uiState.value = _uiState.value.copy(showConfirmDialog = true, loteToDelete = id)
    }

    fun cancelarDesactivar() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false, loteToDelete = null)
    }

    fun confirmarDesactivar() {
        val id = _uiState.value.loteToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = loteRepo.suspenderLote(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showConfirmDialog = false, loteToDelete = null, isLoading = false)
                    cargarDatos()
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

    fun imprimirLote(lote: LoteResponse) {
        viewModelScope.launch {
            try {
                val proveedor = _uiState.value.proveedores.find { it.id == lote.proveedorId }?.nombre ?: "Desconocido"
                val file = generarPDF(lote, proveedor)
                _uiState.value = _uiState.value.copy(error = "PDF generado en ${file.absolutePath}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al generar PDF: ${e.message}")
            }
        }
    }

    // ✅ CORREGIDO: error 'val cannot be reassigned'
    private suspend fun generarPDF(lote: LoteResponse, proveedorNombre: String): File = withContext(Dispatchers.IO) {
        val context = MyApplication.instance
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var currentPage = pdf.startPage(pageInfo)   // primera página
        var canvas = currentPage.canvas             // var, no val

        val paintTitle = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val paintText = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        val paintLine = Paint().apply {
            color = android.graphics.Color.GRAY
            strokeWidth = 1f
        }

        var y = margin

        // Cabecera común
        canvas.drawText("Comprobante de Lote: ${lote.factura}", margin.toFloat(), y.toFloat(), paintTitle)
        y += 30
        canvas.drawText("Proveedor: $proveedorNombre", margin.toFloat(), y.toFloat(), paintText)
        y += 30
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 20
        canvas.drawText("Medicamento", margin.toFloat(), y.toFloat(), paintText)
        canvas.drawText("Cantidad", (pageWidth - margin - 80).toFloat(), y.toFloat(), paintText)
        y += 10
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 15

        for (det in lote.detalles) {
            val med = _uiState.value.medicamentos.find { it.id == det.medicamentoId }
            val nombre = med?.nombre ?: det.medicamentoNombre
            canvas.drawText(nombre, margin.toFloat(), y.toFloat(), paintText)
            canvas.drawText(det.cantidad.toString(), (pageWidth - margin - 80).toFloat(), y.toFloat(), paintText)
            y += 20

            // Si nos quedamos sin espacio, crear nueva página
            if (y > pageHeight - margin) {
                pdf.finishPage(currentPage)
                currentPage = pdf.startPage(pageInfo)
                canvas = currentPage.canvas
                y = margin
                // Repetir cabecera en nueva página
                canvas.drawText("Comprobante de Lote: ${lote.factura} (continuación)", margin.toFloat(), y.toFloat(), paintTitle)
                y += 30
                canvas.drawText("Proveedor: $proveedorNombre", margin.toFloat(), y.toFloat(), paintText)
                y += 30
                canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
                y += 20
                canvas.drawText("Medicamento", margin.toFloat(), y.toFloat(), paintText)
                canvas.drawText("Cantidad", (pageWidth - margin - 80).toFloat(), y.toFloat(), paintText)
                y += 10
                canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
                y += 15
            }
        }

        pdf.finishPage(currentPage)

        val file = File(context.cacheDir, "Lote_${lote.factura}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        file
    }

    private fun filtrarLotes(
        lotes: List<LoteResponse>,
        query: String,
        filtro: String,
        medicamentos: List<MedicamentoResponse>,
        proveedores: List<ProveedorResponse>
    ): List<LoteResponse> {
        val term = query.lowercase().trim()
        return lotes.filter { l ->
            if (!l.activo) return@filter false
            val proveedor = proveedores.find { it.id == l.proveedorId }?.nombre?.lowercase() ?: ""
            val facturaMatch = l.factura?.lowercase()?.contains(term) == true
            val proveedorMatch = proveedor.contains(term)
            val medMatch = l.detalles?.any { det ->
                medicamentos.find { it.id == det.medicamentoId }?.nombre?.lowercase()?.contains(term) == true
            } ?: false
            val matchSearch = term.isEmpty() || facturaMatch || proveedorMatch || medMatch

            val totalStock = l.detalles?.sumOf { it.cantidad } ?: 0
            val matchStock = when (filtro) {
                "stock" -> totalStock > 0
                "agotado" -> totalStock == 0
                else -> true
            }
            matchSearch && matchStock
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val lotesFiltrados: List<LoteResponse>
        get() = filtrarLotes(_uiState.value.lotes, _uiState.value.searchQuery, _uiState.value.filtroStock, _uiState.value.medicamentos, _uiState.value.proveedores)

    val paginatedLotes: List<LoteResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, lotesFiltrados.size)
            if (start >= end) return emptyList()
            return lotesFiltrados.subList(start, end)
        }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = lotesFiltrados.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}