package com.sanidad.movil.presentation.screens.ventas

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CarritoItem(
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val precioUnitario: Double,
    var cantidad: Int,
    val imagen: String? = null
)

data class LoteDetalleParaVenta(
    val id: Long,
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val presentacion: String,
    val precioUnitario: Double,
    val stockVenta: Int,
    val imagen: String? = null,
    val loteNum: String? = null
)

data class VentaCreateUiState(
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val clientes: List<ClienteResponse> = emptyList(),
    val lotes: List<LoteResponse> = emptyList(),
    val recetasDisponibles: List<RecetaResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val resultadosBusqueda: List<LoteDetalleParaVenta> = emptyList(),
    val loteFIFO: LoteDetalleParaVenta? = null,
    val complementarios: List<SugerenciaProductoDTO> = emptyList(),
    val contextoCliente: Any? = null, // simplificado
    val carrito: List<CarritoItem> = emptyList(),
    val clienteSeleccionado: ClienteResponse? = null,
    val recetaSeleccionada: RecetaResponse? = null,
    val montoEfectivo: Double = 0.0,
    val montoUsadoSaldo: Double = 0.0,
    val tipoVenta: String = "rapida",
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val cambio: Double = 0.0,
    val requiereReceta: Boolean = false,
    val showClienteDialog: Boolean = false,
    val showRecetaDialog: Boolean = false
)

class VentaCreateViewModel(
    private val medicamentoRepo: MedicamentoRepository,
    private val clienteRepo: ClienteRepository,
    private val loteRepo: LoteRepository,
    private val recetaRepo: RecetaRepository,
    private val ventaRepo: VentaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentaCreateUiState())
    val uiState: StateFlow<VentaCreateUiState> = _uiState

    var usuarioId: Long = 0L

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val medRes = medicamentoRepo.getMedicamentos()
            val cliRes = clienteRepo.getClientes()
            val lotRes = loteRepo.getLotes()
            val recRes = recetaRepo.getRecetasDisponibles()

            val medicamentos = (medRes as? ApiResult.Success)?.data ?: emptyList()
            val clientes = (cliRes as? ApiResult.Success)?.data ?: emptyList()
            val lotes = (lotRes as? ApiResult.Success)?.data ?: emptyList()
            val recetas = (recRes as? ApiResult.Success)?.data ?: emptyList()

            val consumidor = clientes.find { it.nombre.contains("consumidor", ignoreCase = true) }
            _uiState.value = _uiState.value.copy(
                medicamentos = medicamentos,
                clientes = clientes,
                lotes = lotes,
                recetasDisponibles = recetas,
                isLoading = false,
                clienteSeleccionado = consumidor
            )
            actualizarTotales()
        }
    }

    fun buscarMedicamento(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                resultadosBusqueda = emptyList(),
                loteFIFO = null,
                complementarios = emptyList()
            )
            return
        }
        viewModelScope.launch {
            val medEncontrado = _uiState.value.medicamentos.find {
                it.nombre.contains(query, ignoreCase = true)
            }
            if (medEncontrado != null) {
                // FIFO (simplificado: primer detalle con stock)
                val fifoDetalle = _uiState.value.lotes
                    .flatMap { l -> l.detalles.filter { it.medicamentoId == medEncontrado.id && it.cantidad > 0 } }
                    .minByOrNull { it.id }
                if (fifoDetalle != null) {
                    _uiState.value = _uiState.value.copy(
                        loteFIFO = LoteDetalleParaVenta(
                            id = fifoDetalle.id,
                            medicamentoId = medEncontrado.id,
                            medicamentoNombre = medEncontrado.nombre,
                            presentacion = medEncontrado.presentacion ?: "",
                            precioUnitario = medEncontrado.precioUnitario.toDouble(),
                            stockVenta = fifoDetalle.cantidad,
                            imagen = medEncontrado.imagen
                        )
                    )
                }
                // Complementarios (placeholder)
                _uiState.value = _uiState.value.copy(complementarios = emptyList())
            }
            // Resultados de búsqueda
            val resultados = _uiState.value.lotes.flatMap { l ->
                l.detalles.filter { det ->
                    val med = _uiState.value.medicamentos.find { it.id == det.medicamentoId }
                    med != null && med.nombre.contains(query, ignoreCase = true) && det.cantidad > 0
                }.map { det ->
                    val med = _uiState.value.medicamentos.find { it.id == det.medicamentoId }!!
                    LoteDetalleParaVenta(
                        id = det.id,
                        medicamentoId = med.id,
                        medicamentoNombre = med.nombre,
                        presentacion = med.presentacion ?: "",
                        precioUnitario = med.precioUnitario.toDouble(),
                        stockVenta = det.cantidad,
                        imagen = med.imagen,
                        loteNum = l.numeroLote
                    )
                }
            }
            _uiState.value = _uiState.value.copy(resultadosBusqueda = resultados.take(6))
        }
    }

    fun agregarAlCarrito(item: LoteDetalleParaVenta) {
        val carrito = _uiState.value.carrito.toMutableList()
        val existente = carrito.find { it.loteDetalleId == item.id }
        if (existente != null) {
            if (existente.cantidad < item.stockVenta) existente.cantidad++
        } else {
            carrito.add(CarritoItem(
                loteDetalleId = item.id,
                medicamentoNombre = item.medicamentoNombre,
                precioUnitario = item.precioUnitario,
                cantidad = 1,
                imagen = item.imagen
            ))
        }
        _uiState.value = _uiState.value.copy(
            carrito = carrito,
            query = "",
            resultadosBusqueda = emptyList(),
            loteFIFO = null
        )
        actualizarTotales()
    }

    fun eliminarDelCarrito(index: Int) {
        val carrito = _uiState.value.carrito.toMutableList()
        carrito.removeAt(index)
        _uiState.value = _uiState.value.copy(carrito = carrito)
        actualizarTotales()
    }

    fun actualizarCantidadCarrito(index: Int, nuevaCantidad: Int) {
        val carrito = _uiState.value.carrito.toMutableList()
        if (index in carrito.indices) {
            val item = carrito[index]
            val stock = getStockVenta(item.loteDetalleId)
            if (nuevaCantidad <= stock) {
                carrito[index] = item.copy(cantidad = nuevaCantidad)
                _uiState.value = _uiState.value.copy(carrito = carrito)
                actualizarTotales()
            }
        }
    }

    private fun getStockVenta(loteDetalleId: Long): Int {
        for (lote in _uiState.value.lotes) {
            val det = lote.detalles.find { it.id == loteDetalleId }
            if (det != null) return det.cantidad
        }
        return 0
    }

    fun seleccionarCliente(cliente: ClienteResponse?) {
        _uiState.value = _uiState.value.copy(
            clienteSeleccionado = cliente,
            tipoVenta = if (cliente != null) "cliente" else "rapida",
            showClienteDialog = false,
            contextoCliente = null // podrías cargar contexto aquí
        )
    }

    fun seleccionarReceta(receta: RecetaResponse?) {
        _uiState.value = _uiState.value.copy(recetaSeleccionada = receta, showRecetaDialog = false)
    }

    fun setMontoEfectivo(monto: Double) {
        _uiState.value = _uiState.value.copy(montoEfectivo = monto)
        actualizarTotales()
    }

    fun setMontoUsadoSaldo(monto: Double) {
        _uiState.value = _uiState.value.copy(montoUsadoSaldo = monto)
        actualizarTotales()
    }

    fun crearVenta(onSuccess: (VentaResponse) -> Unit) {
        val state = _uiState.value
        if (state.carrito.isEmpty()) {
            _uiState.value = state.copy(error = "El carrito está vacío")
            return
        }
        // Verificar stock
        for (item in state.carrito) {
            val stock = getStockVenta(item.loteDetalleId)
            if (item.cantidad > stock) {
                _uiState.value = state.copy(error = "Stock insuficiente para ${item.medicamentoNombre}. Disponible: $stock")
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val detalles = state.carrito.map {
                VentaDetalleRequest(loteDetalleId = it.loteDetalleId, cantidad = it.cantidad)
            }
            val request = VentaRequest(
                clienteId = state.clienteSeleccionado?.id,
                usuarioId = usuarioId,
                detalles = detalles,
                recetaId = state.recetaSeleccionada?.id,
                montoUsadoSaldo = state.montoUsadoSaldo,
                montoEfectivo = state.montoEfectivo
            )
            when (val result = ventaRepo.crearVenta(request)) {
                is ApiResult.Success -> {
                    val venta = result.data
                    // Generar PDF ticket
                    generarTicket(venta, state.carrito, state.medicamentos)
                    // Limpiar
                    _uiState.value = _uiState.value.copy(
                        carrito = emptyList(),
                        montoEfectivo = 0.0,
                        montoUsadoSaldo = 0.0,
                        recetaSeleccionada = null,
                        isLoading = false
                    )
                    onSuccess(venta)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun actualizarTotales() {
        val subtotal = _uiState.value.carrito.sumOf { it.precioUnitario * it.cantidad }
        val total = subtotal * 1.15
        val cambio = ((_uiState.value.montoEfectivo + _uiState.value.montoUsadoSaldo) - total).coerceAtLeast(0.0)
        val requiereReceta = _uiState.value.carrito.any { item ->
            val det = _uiState.value.lotes.flatMap { it.detalles }.find { it.id == item.loteDetalleId }
            val med = det?.let { _uiState.value.medicamentos.find { m -> m.id == it.medicamentoId } }
            med?.receta == true
        }
        _uiState.value = _uiState.value.copy(
            subtotal = subtotal,
            total = total,
            cambio = cambio,
            requiereReceta = requiereReceta
        )
    }

    private suspend fun generarTicket(venta: VentaResponse, items: List<CarritoItem>, medicamentos: List<MedicamentoResponse>) =
        withContext(Dispatchers.IO) {
            val context = MyApplication.instance
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(227, 400, 1).create() // 80mm ancho aprox
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD }
            var y = 20
            canvas.drawText("FARMACIA SANIDAD", 113.5f, y.toFloat(), paint.apply { textAlign = Paint.Align.CENTER })
            y += 12
            paint.textSize = 7f
            canvas.drawText("Factura: ${venta.numeroFactura}", 10f, y.toFloat(), paint)
            y += 10
            canvas.drawText("Cliente: ${venta.clienteNombre ?: "Consumidor Final"}", 10f, y.toFloat(), paint)
            y += 10
            canvas.drawText("--------------------------------", 10f, y.toFloat(), paint)
            y += 10
            for (item in items) {
                canvas.drawText("${item.medicamentoNombre} x${item.cantidad}", 10f, y.toFloat(), paint)
                canvas.drawText("C$ ${"%.2f".format(item.precioUnitario * item.cantidad)}", 180f, y.toFloat(), paint.apply { textAlign = Paint.Align.RIGHT })
                y += 10
            }
            canvas.drawText("--------------------------------", 10f, y.toFloat(), paint)
            y += 10
            canvas.drawText("TOTAL: C$ ${"%.2f".format(venta.total)}", 10f, y.toFloat(), paint)
            pdf.finishPage(page)
            val file = File(context.cacheDir, "Venta_${venta.numeroFactura}.pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()
        }

    fun setShowClienteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClienteDialog = show)
    }

    fun setShowRecetaDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showRecetaDialog = show)
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}