package com.sanidad.movil.presentation.screens.devolucionesProveedor

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.DevolucionProveedorRepository
import com.sanidad.movil.data.repository.LoteRepository
import com.sanidad.movil.data.repository.MedicamentoRepository
import com.sanidad.movil.data.repository.ProveedorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ---------- DATA CLASSES ----------
data class ItemDevolucionProv(
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val cantidadDisponible: Int,
    val cantidadDevuelta: Int = 0,
    val imagen: String? = null
)

data class DevolucionesProveedorUiState(
    val devoluciones: List<DevolucionProveedorResponse> = emptyList(),
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val proveedores: List<ProveedorResponse> = emptyList(),
    val lotes: List<LoteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val estadoFiltro: String = "TODOS",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val showDrawer: Boolean = false,
    val busquedaLote: String = "",
    val loteSeleccionado: LoteResponse? = null,
    val itemsDevolucion: List<ItemDevolucionProv> = emptyList(),
    val motivo: String = "",

    val showAprobarDialog: Boolean = false,
    val devolucionAprobarId: Long? = null,
    val showRechazarDialog: Boolean = false,
    val devolucionRechazarId: Long? = null,
    val motivoRechazo: String = ""
)

class DevolucionesProveedorViewModel(
    private val devolucionProveedorRepo: DevolucionProveedorRepository,
    private val medicamentoRepo: MedicamentoRepository,
    private val proveedorRepo: ProveedorRepository,
    private val loteRepo: LoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionesProveedorUiState())
    val uiState: StateFlow<DevolucionesProveedorUiState> = _uiState.asStateFlow()

    private val rowsPerPage = 15

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val devResult = devolucionProveedorRepo.getDevoluciones()
                val medResult = medicamentoRepo.getMedicamentos()
                val provResult = proveedorRepo.getProveedores()
                val lotResult = loteRepo.getLotes()

                val devoluciones = when (devResult) {
                    is ApiResult.Success -> devResult.data.sortedByDescending { it.id }
                    else -> emptyList()
                }
                val medicamentos = when (medResult) {
                    is ApiResult.Success -> medResult.data
                    else -> emptyList()
                }
                val proveedores = when (provResult) {
                    is ApiResult.Success -> provResult.data
                    else -> emptyList()
                }
                val lotes = when (lotResult) {
                    is ApiResult.Success -> lotResult.data
                    else -> emptyList()
                }

                val filtered = filtrarDevoluciones(devoluciones, _uiState.value.searchTerm, _uiState.value.estadoFiltro)
                _uiState.value = _uiState.value.copy(
                    devoluciones = devoluciones,
                    medicamentos = medicamentos,
                    proveedores = proveedores,
                    lotes = lotes,
                    isLoading = false,
                    totalPages = calcularTotalPaginas(filtered.size),
                    currentPage = 1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Error al cargar datos: ${e.message}")
            }
        }
    }

    fun setSearch(term: String) {
        _uiState.value = _uiState.value.copy(searchTerm = term, currentPage = 1)
        actualizarPaginacion()
    }

    fun setEstadoFiltro(estado: String) {
        _uiState.value = _uiState.value.copy(estadoFiltro = estado, currentPage = 1)
        actualizarPaginacion()
    }

    fun setPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    fun abrirNuevaDevolucion() {
        _uiState.value = _uiState.value.copy(
            showDrawer = true,
            loteSeleccionado = null,
            itemsDevolucion = emptyList(),
            motivo = "",
            busquedaLote = ""
        )
        viewModelScope.launch {
            try {
                when (val res = loteRepo.getLotes()) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(lotes = res.data)
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al cargar lotes: ${e.message}")
            }
        }
    }

    fun cerrarDrawer() {
        _uiState.value = _uiState.value.copy(showDrawer = false)
    }

    fun setBusquedaLote(term: String) {
        _uiState.value = _uiState.value.copy(busquedaLote = term)
    }

    fun seleccionarLote(lote: LoteResponse) {
        viewModelScope.launch {
            try {
                when (val res = loteRepo.getLote(lote.id)) {
                    is ApiResult.Success -> {
                        val loteCompleto = res.data
                        val detalles = loteCompleto.detalles.mapNotNull { det ->
                            try {
                                val med = _uiState.value.medicamentos.find { it.id == det.medicamentoId }
                                ItemDevolucionProv(
                                    loteDetalleId = det.id,
                                    medicamentoNombre = med?.nombre ?: det.medicamentoNombre,
                                    cantidadDisponible = det.cantidad,
                                    cantidadDevuelta = 0,
                                    imagen = med?.imagen
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _uiState.value = _uiState.value.copy(
                            loteSeleccionado = lote,
                            itemsDevolucion = detalles
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al cargar detalles del lote: ${e.message}")
            }
        }
    }

    fun actualizarCantidad(loteDetalleId: Long, cantidad: Int) {
        try {
            val nuevosItems = _uiState.value.itemsDevolucion.map {
                if (it.loteDetalleId == loteDetalleId) it.copy(cantidadDevuelta = cantidad.coerceIn(0, it.cantidadDisponible))
                else it
            }
            _uiState.value = _uiState.value.copy(itemsDevolucion = nuevosItems)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Error al actualizar cantidad: ${e.message}")
        }
    }

    fun setMotivo(motivo: String) {
        _uiState.value = _uiState.value.copy(motivo = motivo)
    }

    fun solicitarDevolucion(usuarioId: Long) {
        viewModelScope.launch {
            try {
                val detalles = _uiState.value.itemsDevolucion
                    .filter { it.cantidadDevuelta > 0 }
                    .map { DevolucionProveedorDetalleRequest(it.loteDetalleId, it.cantidadDevuelta) }

                if (detalles.isEmpty()) {
                    _uiState.value = _uiState.value.copy(error = "Seleccione cantidades válidas")
                    return@launch
                }

                val request = DevolucionProveedorRequest(
                    loteId = _uiState.value.loteSeleccionado!!.id,
                    solicitadoPorId = usuarioId,
                    motivo = _uiState.value.motivo.ifBlank { null },
                    detalles = detalles
                )
                when (val res = devolucionProveedorRepo.solicitarDevolucion(request)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(showDrawer = false)
                        cargarDatos()
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al solicitar devolución: ${e.message}")
            }
        }
    }

    fun mostrarAprobarDialog(id: Long) {
        _uiState.value = _uiState.value.copy(showAprobarDialog = true, devolucionAprobarId = id)
    }

    fun ocultarAprobarDialog() {
        _uiState.value = _uiState.value.copy(showAprobarDialog = false)
    }

    fun confirmarAprobar(usuarioId: Long) {
        val id = _uiState.value.devolucionAprobarId ?: return
        viewModelScope.launch {
            try {
                val request = DevolucionProveedorAprobarRequest(
                    devolucionId = id,
                    aprobadoPorId = usuarioId,
                    aprobada = true,
                    motivoRechazo = null
                )
                when (val res = devolucionProveedorRepo.aprobarDevolucion(request)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(showAprobarDialog = false)
                        cargarDatos()
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al aprobar: ${e.message}")
            }
        }
    }

    fun mostrarRechazarDialog(id: Long) {
        _uiState.value = _uiState.value.copy(showRechazarDialog = true, devolucionRechazarId = id, motivoRechazo = "")
    }

    fun ocultarRechazarDialog() {
        _uiState.value = _uiState.value.copy(showRechazarDialog = false, motivoRechazo = "")
    }

    fun setMotivoRechazo(motivo: String) {
        _uiState.value = _uiState.value.copy(motivoRechazo = motivo)
    }

    fun confirmarRechazar(usuarioId: Long) {
        val id = _uiState.value.devolucionRechazarId ?: return
        viewModelScope.launch {
            try {
                val request = DevolucionProveedorAprobarRequest(
                    devolucionId = id,
                    aprobadoPorId = usuarioId,
                    aprobada = false,
                    motivoRechazo = _uiState.value.motivoRechazo
                )
                when (val res = devolucionProveedorRepo.aprobarDevolucion(request)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(showRechazarDialog = false)
                        cargarDatos()
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al rechazar: ${e.message}")
            }
        }
    }

    // PDF y WhatsApp
    fun imprimirPDF(devolucion: DevolucionProveedorResponse) {
        viewModelScope.launch {
            try {
                val proveedor = _uiState.value.proveedores.find { it.nombre == devolucion.proveedorNombre }
                val file = generarPDF(devolucion, proveedor?.nombre ?: devolucion.proveedorNombre, proveedor?.telefono)
                _uiState.value = _uiState.value.copy(error = "PDF generado en ${file.absolutePath}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al generar PDF: ${e.message}")
            }
        }
    }

    fun enviarWhatsApp(devolucion: DevolucionProveedorResponse) {
        viewModelScope.launch {
            try {
                val proveedor = _uiState.value.proveedores.find { it.nombre == devolucion.proveedorNombre }
                val telefono = proveedor?.telefono
                if (telefono.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(error = "No se encontró el teléfono del proveedor")
                    return@launch
                }
                val productosTexto = devolucion.detalles.joinToString("\n") { det ->
                    val med = obtenerMedicamentoDesdeDetalle(det)
                    val nombre = med?.nombre ?: det.medicamentoNombre
                    "- $nombre: ${det.cantidadDevuelta}"
                }
                val mensaje = "*HOLA, REPORTE DE DEVOLUCIÓN*\n\n" +
                        "*N° Solicitud:* ${devolucion.numeroDevolucion ?: "Pendiente"}\n" +
                        "*Factura Lote:* ${devolucion.numeroFacturaLote}\n" +
                        "*Proveedor:* ${devolucion.proveedorNombre}\n" +
                        "*Estado:* ${devolucion.estado}\n" +
                        "*Motivo:* ${devolucion.motivo ?: "No especificado"}\n\n" +
                        "*Productos devueltos:*\n$productosTexto\n\n" +
                        "_Se ha generado un PDF con los detalles completos._"
                abrirWhatsApp(telefono, mensaje)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error: ${e.message}")
            }
        }
    }

    fun solicitarYEnviarWhatsApp(usuarioId: Long) {
        viewModelScope.launch {
            try {
                val detalles = _uiState.value.itemsDevolucion
                    .filter { it.cantidadDevuelta > 0 }
                    .map { DevolucionProveedorDetalleRequest(it.loteDetalleId, it.cantidadDevuelta) }

                if (detalles.isEmpty()) {
                    _uiState.value = _uiState.value.copy(error = "Seleccione cantidades válidas")
                    return@launch
                }

                val request = DevolucionProveedorRequest(
                    loteId = _uiState.value.loteSeleccionado!!.id,
                    solicitadoPorId = usuarioId,
                    motivo = _uiState.value.motivo.ifBlank { null },
                    detalles = detalles
                )

                when (val res = devolucionProveedorRepo.solicitarDevolucion(request)) {
                    is ApiResult.Success -> {
                        val lote = _uiState.value.loteSeleccionado
                        val proveedor = _uiState.value.proveedores.find { it.id == lote?.proveedorId }
                        val telefono = proveedor?.telefono

                        if (!telefono.isNullOrBlank()) {
                            try {
                                val productosTexto = _uiState.value.itemsDevolucion
                                    .filter { it.cantidadDevuelta > 0 }
                                    .joinToString("\n") { "- ${it.medicamentoNombre}: ${it.cantidadDevuelta}" }
                                val mensaje = "*HOLA, SOLICITUD DE DEVOLUCIÓN*\n\n" +
                                        "Se ha generado una solicitud para el lote *${lote?.factura}*.\n\n" +
                                        "*Detalles:*\n$productosTexto\n\n" +
                                        "*Motivo:* ${_uiState.value.motivo.ifBlank { "No especificado" }}\n\n" +
                                        "_Se adjuntará el comprobante en PDF._"
                                abrirWhatsApp(telefono, mensaje)
                            } catch (e: Exception) {
                                _uiState.value = _uiState.value.copy(error = "Error al enviar WhatsApp: ${e.message}")
                            }
                        }
                        _uiState.value = _uiState.value.copy(showDrawer = false)
                        cargarDatos()
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al solicitar: ${e.message}")
            }
        }
    }

    private suspend fun generarPDF(
        devolucion: DevolucionProveedorResponse,
        proveedorNombre: String,
        proveedorTelefono: String?
    ): File = withContext(Dispatchers.IO) {
        val context = MyApplication.instance
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            color = android.graphics.Color.rgb(37, 99, 235)
            textSize = 18f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val paintText = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        val paintHeader = Paint().apply {
            color = android.graphics.Color.rgb(100, 100, 100)
            textSize = 10f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val paintLine = Paint().apply {
            color = android.graphics.Color.rgb(200, 200, 200)
            strokeWidth = 1f
        }

        var y = margin

        canvas.drawText("FarmaSystem - Devolución a Proveedor", margin.toFloat(), y.toFloat(), paintTitle)
        y += 30
        canvas.drawText("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", margin.toFloat(), y.toFloat(), paintHeader)
        y += 20
        canvas.drawText("Solicitado por: ${devolucion.solicitadoPorNombre}", margin.toFloat(), y.toFloat(), paintHeader)  // ← CORREGIDO
        y += 20

        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 15

        canvas.drawText("Proveedor: $proveedorNombre", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        canvas.drawText("Factura Lote: ${devolucion.numeroFacturaLote ?: "N/A"}", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        canvas.drawText("N° Solicitud: ${devolucion.numeroDevolucion ?: "Pendiente"}", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        if (proveedorTelefono != null) {
            canvas.drawText("Teléfono: $proveedorTelefono", margin.toFloat(), y.toFloat(), paintText)
            y += 18
        }

        y += 10

        for (det in devolucion.detalles) {
            val med = obtenerMedicamentoDesdeDetalle(det)
            val nombre = med?.nombre ?: det.medicamentoNombre
            canvas.drawText("$nombre x${det.cantidadDevuelta}", margin.toFloat(), y.toFloat(), paintText)
            y += 20
            if (y > pageHeight - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = margin
            }
        }

        y += 15
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 15
        canvas.drawText("Motivo: ${devolucion.motivo ?: "No especificado"}", margin.toFloat(), y.toFloat(), paintText)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Devolucion_${devolucion.numeroDevolucion}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()
        file
    }

    private fun abrirWhatsApp(telefono: String, mensaje: String) {
        val context = MyApplication.instance
        val url = "https://wa.me/${telefono}?text=${Uri.encode(mensaje)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "No se pudo abrir WhatsApp")
        }
    }

    // Utilidades
    private fun filtrarDevoluciones(
        lista: List<DevolucionProveedorResponse>,
        term: String,
        estado: String
    ): List<DevolucionProveedorResponse> {
        val lower = term.lowercase().trim()
        return lista.filter { d ->
            val matchSearch = lower.isEmpty() ||
                    d.numeroDevolucion?.lowercase()?.contains(lower) == true ||
                    d.numeroFacturaLote?.lowercase()?.contains(lower) == true ||
                    d.proveedorNombre?.lowercase()?.contains(lower) == true
            val matchEstado = estado == "TODOS" || d.estado == estado
            matchSearch && matchEstado
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val devolucionesFiltradas: List<DevolucionProveedorResponse>
        get() = filtrarDevoluciones(_uiState.value.devoluciones, _uiState.value.searchTerm, _uiState.value.estadoFiltro)

    val paginatedDevoluciones: List<DevolucionProveedorResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, devolucionesFiltradas.size)
            if (start >= end) return emptyList()
            return devolucionesFiltradas.subList(start, end)
        }

    val lotesFiltrados: List<LoteResponse>
        get() {
            val term = _uiState.value.busquedaLote.lowercase().trim()
            return _uiState.value.lotes.filter { it.factura?.lowercase()?.contains(term) == true }
        }

    fun obtenerMedicamentoDesdeDetalle(det: DevolucionProveedorDetalleResponse): MedicamentoResponse? {
        return try {
            val lotes = _uiState.value.lotes
            val medicamentos = _uiState.value.medicamentos
            if (det.loteDetalleId != null) {
                val loteDet = lotes.flatMap { it.detalles }.find { it.id == det.loteDetalleId }
                if (loteDet != null) {
                    medicamentos.find { it.id == loteDet.medicamentoId }
                } else null
            } else {
                medicamentos.find { it.nombre.equals(det.medicamentoNombre, ignoreCase = true) }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = devolucionesFiltradas.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}