package com.sanidad.movil.presentation.screens.devolucionesProveedor

import android.content.Intent
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// ---------- DATA CLASSES ----------
data class ItemDevolucionProv(
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val cantidadDisponible: Int,
    var cantidadDevuelta: Int,
    val imagen: String? = null
)

data class DevolucionesProveedorUiState(
    val devoluciones: List<DevolucionProveedorResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val estadoFiltro: String = "TODOS",
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val showDrawer: Boolean = false,
    val lotes: List<LoteResponse> = emptyList(),
    val busquedaLote: String = "",
    val loteSeleccionado: LoteResponse? = null,
    val itemsDevolucion: List<ItemDevolucionProv> = emptyList(),
    val motivo: String = "",
    val proveedores: List<ProveedorResponse> = emptyList(),
    // Diálogos
    val showAprobarDialog: Boolean = false,
    val showRechazarDialog: Boolean = false,
    val motivoRechazo: String = "",
    val devolucionIdEnAccion: Long? = null
)

// ---------- VIEWMODEL ----------
class DevolucionesProveedorViewModel(
    private val devolucionProveedorRepo: DevolucionProveedorRepository,
    private val medicamentoRepo: MedicamentoRepository,
    private val proveedorRepo: ProveedorRepository,
    private val loteRepo: LoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionesProveedorUiState())
    val uiState: StateFlow<DevolucionesProveedorUiState> = _uiState.asStateFlow()

    private val medicamentoCache = mutableMapOf<Long, MedicamentoResponse>()

    // Propiedades derivadas
    val paginatedDevoluciones: List<DevolucionProveedorResponse>
        get() {
            val filtered = _uiState.value.devoluciones.filter { dev ->
                (dev.numeroDevolucion.contains(_uiState.value.searchTerm, ignoreCase = true) ||
                        (dev.numeroFacturaLote?.contains(_uiState.value.searchTerm, ignoreCase = true) == true) ||
                        dev.proveedorNombre.contains(_uiState.value.searchTerm, ignoreCase = true)) &&
                        (_uiState.value.estadoFiltro == "TODOS" || dev.estado == _uiState.value.estadoFiltro)
            }
            val start = (_uiState.value.currentPage - 1) * _uiState.value.pageSize
            val end = minOf(start + _uiState.value.pageSize, filtered.size)
            return if (start < filtered.size) filtered.subList(start, end) else emptyList()
        }

    val lotesFiltrados: List<LoteResponse>
        get() = _uiState.value.lotes.filter {
            it.factura?.contains(_uiState.value.busquedaLote, ignoreCase = true) == true
        }

    val totalPages: Int
        get() {
            val filtered = _uiState.value.devoluciones.filter { dev ->
                (dev.numeroDevolucion.contains(_uiState.value.searchTerm, ignoreCase = true) ||
                        (dev.numeroFacturaLote?.contains(_uiState.value.searchTerm, ignoreCase = true) == true) ||
                        dev.proveedorNombre.contains(_uiState.value.searchTerm, ignoreCase = true)) &&
                        (_uiState.value.estadoFiltro == "TODOS" || dev.estado == _uiState.value.estadoFiltro)
            }
            return (filtered.size + _uiState.value.pageSize - 1) / _uiState.value.pageSize
        }

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Obtener devoluciones
                val devResp = devolucionProveedorRepo.getDevoluciones()
                if (devResp is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(devoluciones = devResp.data)
                } else if (devResp is ApiResult.Error) {
                    _uiState.value = _uiState.value.copy(error = devResp.message)
                }

                // Obtener proveedores
                val proveedoresResp = proveedorRepo.getProveedores()
                if (proveedoresResp is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(proveedores = proveedoresResp.data)
                } else if (proveedoresResp is ApiResult.Error) {
                    _uiState.value = _uiState.value.copy(error = proveedoresResp.message)
                }

                // Obtener lotes
                val lotesResp = loteRepo.getLotes()
                if (lotesResp is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(lotes = lotesResp.data)
                } else if (lotesResp is ApiResult.Error) {
                    _uiState.value = _uiState.value.copy(error = lotesResp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSearch(term: String) {
        _uiState.value = _uiState.value.copy(searchTerm = term, currentPage = 1)
    }

    fun setEstadoFiltro(estado: String) {
        _uiState.value = _uiState.value.copy(estadoFiltro = estado, currentPage = 1)
    }

    fun setPage(page: Int) {
        if (page in 1..totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun abrirNuevaDevolucion() {
        _uiState.value = _uiState.value.copy(
            showDrawer = true,
            loteSeleccionado = null,
            busquedaLote = "",
            itemsDevolucion = emptyList(),
            motivo = ""
        )
    }

    fun cerrarDrawer() {
        _uiState.value = _uiState.value.copy(showDrawer = false)
    }

    fun setBusquedaLote(query: String) {
        _uiState.value = _uiState.value.copy(busquedaLote = query)
    }

    fun seleccionarLote(lote: LoteResponse) {
        _uiState.value = _uiState.value.copy(loteSeleccionado = lote)
        cargarItemsDesdeLote(lote)
    }

    private fun cargarItemsDesdeLote(lote: LoteResponse) {
        viewModelScope.launch {
            val items = lote.detalles.map { detalle ->
                val med = obtenerMedicamento(detalle.medicamentoId)
                ItemDevolucionProv(
                    loteDetalleId = detalle.id,
                    medicamentoNombre = detalle.medicamentoNombre,
                    cantidadDisponible = detalle.cantidad,
                    cantidadDevuelta = 0,
                    imagen = med?.imagen
                )
            }
            _uiState.value = _uiState.value.copy(itemsDevolucion = items)
        }
    }

    private suspend fun obtenerMedicamento(medicamentoId: Long): MedicamentoResponse? {
        return medicamentoCache[medicamentoId] ?: run {
            val res = medicamentoRepo.getMedicamento(medicamentoId)
            if (res is ApiResult.Success) {
                medicamentoCache[medicamentoId] = res.data
                res.data
            } else null
        }
    }

    fun obtenerMedicamentoDesdeDetalle(det: DevolucionProveedorDetalleResponse): MedicamentoResponse? {
        // Como DevolucionProveedorDetalleResponse no tiene medicamentoId, devolvemos null
        return null
    }

    fun actualizarCantidad(loteDetalleId: Long, cantidad: Int) {
        val nuevosItems = _uiState.value.itemsDevolucion.map {
            if (it.loteDetalleId == loteDetalleId) it.copy(cantidadDevuelta = cantidad.coerceIn(0, it.cantidadDisponible))
            else it
        }
        _uiState.value = _uiState.value.copy(itemsDevolucion = nuevosItems)
    }

    fun setMotivo(motivo: String) {
        _uiState.value = _uiState.value.copy(motivo = motivo)
    }

    fun solicitarDevolucion(usuarioId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val detalles = _uiState.value.itemsDevolucion.filter { it.cantidadDevuelta > 0 }
                .map { DevolucionProveedorDetalleRequest(it.loteDetalleId, it.cantidadDevuelta) }
            if (detalles.isEmpty()) {
                onError("Seleccione al menos un producto")
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
                    onSuccess()
                    cargarDatos()
                    cerrarDrawer()
                }
                is ApiResult.Error -> onError(res.message)
                else -> {}
            }
        }
    }

    // Diálogos Aprobar
    fun mostrarAprobarDialog(devolucionId: Long) {
        _uiState.value = _uiState.value.copy(
            showAprobarDialog = true,
            devolucionIdEnAccion = devolucionId
        )
    }

    fun ocultarAprobarDialog() {
        _uiState.value = _uiState.value.copy(showAprobarDialog = false, devolucionIdEnAccion = null)
    }

    fun confirmarAprobar(usuarioId: Long) {
        val id = _uiState.value.devolucionIdEnAccion ?: return
        viewModelScope.launch {
            val request = DevolucionProveedorAprobarRequest(
                devolucionId = id,
                aprobadoPorId = usuarioId,
                aprobada = true   // Campo requerido por tu DTO
            )
            when (val res = devolucionProveedorRepo.aprobarDevolucion(request)) {
                is ApiResult.Success -> {
                    cargarDatos()
                    ocultarAprobarDialog()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                    ocultarAprobarDialog()
                }
                else -> {}
            }
        }
    }

    // NOTA: El rechazo no está implementado en tu backend, así que no incluimos diálogo de rechazo

    // PDF y WhatsApp
    fun imprimirPDF(devolucion: DevolucionProveedorResponse) {
        viewModelScope.launch {
            try {
                generarPDF(devolucion)
                _uiState.value = _uiState.value.copy(error = "PDF generado correctamente")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al generar PDF: ${e.message}")
            }
        }
    }

    fun enviarWhatsApp(devolucion: DevolucionProveedorResponse) {
        viewModelScope.launch {
            val telefono = devolucion.proveedorTelefono
            if (telefono.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(error = "No se encontró el teléfono del proveedor")
                return@launch
            }
            try {
                generarPDF(devolucion)
                val productosTexto = devolucion.detalles.joinToString("\n") { "- ${it.medicamentoNombre}: ${it.cantidadDevuelta}" }
                val mensaje = "*HOLA, REPORTE DE DEVOLUCIÓN*\n\n" +
                        "*N° Solicitud:* ${devolucion.numeroDevolucion}\n" +
                        "*Factura Lote:* ${devolucion.numeroFacturaLote ?: "N/A"}\n" +
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
        solicitarDevolucion(usuarioId,
            onSuccess = {
                val lote = _uiState.value.loteSeleccionado
                val proveedor = _uiState.value.proveedores.find { it.id == lote?.proveedorId }
                val telefono = proveedor?.telefono
                if (!telefono.isNullOrBlank()) {
                    val productosTexto = _uiState.value.itemsDevolucion
                        .filter { it.cantidadDevuelta > 0 }
                        .joinToString("\n") { "- ${it.medicamentoNombre}: ${it.cantidadDevuelta}" }
                    val mensaje = "*HOLA, SOLICITUD DE DEVOLUCIÓN*\n\n" +
                            "Se ha generado una solicitud para el lote *${lote?.factura ?: "N/A"}*.\n\n" +
                            "*Detalles:*\n$productosTexto\n\n" +
                            "*Motivo:* ${_uiState.value.motivo.ifBlank { "No especificado" }}\n\n" +
                            "_Se adjuntará el comprobante en PDF._"
                    abrirWhatsApp(telefono, mensaje)
                }
            },
            onError = { error -> _uiState.value = _uiState.value.copy(error = error) }
        )
    }

    private suspend fun generarPDF(devolucion: DevolucionProveedorResponse) = withContext(kotlinx.coroutines.Dispatchers.IO) {
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
        canvas.drawText("Solicitado por: ${devolucion.solicitadoPorNombre}", margin.toFloat(), y.toFloat(), paintHeader)
        y += 20

        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paintLine)
        y += 15

        canvas.drawText("Proveedor: ${devolucion.proveedorNombre}", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        canvas.drawText("Factura Lote: ${devolucion.numeroFacturaLote ?: "N/A"}", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        canvas.drawText("N° Solicitud: ${devolucion.numeroDevolucion}", margin.toFloat(), y.toFloat(), paintText)
        y += 18
        if (devolucion.proveedorTelefono != null) {
            canvas.drawText("Teléfono: ${devolucion.proveedorTelefono}", margin.toFloat(), y.toFloat(), paintText)
            y += 18
        }

        y += 10

        for (det in devolucion.detalles) {
            canvas.drawText("${det.medicamentoNombre} x${det.cantidadDevuelta}", margin.toFloat(), y.toFloat(), paintText)
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
}