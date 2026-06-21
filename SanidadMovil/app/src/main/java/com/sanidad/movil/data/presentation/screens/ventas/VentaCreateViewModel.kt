package com.sanidad.movil.presentation.screens.ventas

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    val loteNum: String? = null,
    /** Fecha de vencimiento del lote (ISO-8601 o null) */
    val fechaVencimiento: String? = null
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
    /** Lote recomendado por FIFO (primero en vencer con stock) */
    val loteFIFO: LoteDetalleParaVenta? = null,
    val carrito: List<CarritoItem> = emptyList(),
    val clienteSeleccionado: ClienteResponse? = null,
    val recetaSeleccionada: RecetaResponse? = null,
    val montoEfectivo: Double = 0.0,
    val montoUsadoSaldo: Double = 0.0,
    val tipoVenta: String = "rapida",
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val cambio: Double = 0.0,
    /** true cuando efectivo + saldo cubre el total */
    val pagoSuficiente: Boolean = false,
    val requiereReceta: Boolean = false,
    val showClienteDialog: Boolean = false,
    val showRecetaDialog: Boolean = false,
    /** Venta recién creada, para mostrar comprobante */
    val ventaExitosa: VentaResponse? = null
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

    // ── Carga inicial ────────────────────────────────────────────────────────

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val medRes = medicamentoRepo.getMedicamentos()
            val cliRes = clienteRepo.getClientes()
            val lotRes = loteRepo.getLotes()
            val recRes = recetaRepo.getRecetasDisponibles()

            val medicamentos = (medRes as? ApiResult.Success)?.data ?: emptyList()
            val clientes    = (cliRes as? ApiResult.Success)?.data ?: emptyList()
            val lotes       = (lotRes as? ApiResult.Success)?.data ?: emptyList()
            val recetas     = (recRes as? ApiResult.Success)?.data ?: emptyList()

            // Pre-seleccionar "Consumidor Final"
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

    // ── Búsqueda + FIFO ──────────────────────────────────────────────────────

    fun buscarMedicamento(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                resultadosBusqueda = emptyList(),
                loteFIFO = null
            )
            return
        }
        viewModelScope.launch {
            val state = _uiState.value

            // Todos los detalles con stock que coincidan con la búsqueda
            val resultados = state.lotes.flatMap { lote ->
                lote.detalles
                    .filter { det ->
                        det.cantidad > 0 &&
                                state.medicamentos.any { m ->
                                    m.id == det.medicamentoId &&
                                            m.nombre.contains(query, ignoreCase = true)
                                }
                    }
                    .map { det ->
                        val med = state.medicamentos.first { it.id == det.medicamentoId }
                        LoteDetalleParaVenta(
                            id = det.id,
                            medicamentoId = med.id,
                            medicamentoNombre = med.nombre,
                            presentacion = med.presentacion ?: "",
                            precioUnitario = med.precioUnitario.toDouble(),
                            stockVenta = det.cantidad,
                            imagen = med.imagen,
                            loteNum = lote.numeroLote,
                            // Asume que LoteResponse expone fechaVencimiento; ajusta si difiere
                            fechaVencimiento = lote.fechaVencimiento
                        )
                    }
            }

            // FIFO real: el lote con la fecha de vencimiento más próxima (null al final)
            val fifo = resultados
                .sortedWith(compareBy(nullsLast()) { it.fechaVencimiento })
                .firstOrNull()

            _uiState.value = _uiState.value.copy(
                resultadosBusqueda = resultados.take(6),
                loteFIFO = if (resultados.size > 1) fifo else null  // solo mostrar si hay >1 opción
            )
        }
    }

    // ── Carrito ──────────────────────────────────────────────────────────────

    fun agregarAlCarrito(item: LoteDetalleParaVenta) {
        val carrito = _uiState.value.carrito.toMutableList()
        val existente = carrito.indexOfFirst { it.loteDetalleId == item.id }
        if (existente >= 0) {
            val actual = carrito[existente]
            if (actual.cantidad < item.stockVenta) {
                carrito[existente] = actual.copy(cantidad = actual.cantidad + 1)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Stock máximo alcanzado para ${item.medicamentoNombre} (${item.stockVenta})"
                )
                return
            }
        } else {
            carrito.add(
                CarritoItem(
                    loteDetalleId = item.id,
                    medicamentoNombre = item.medicamentoNombre,
                    precioUnitario = item.precioUnitario,
                    cantidad = 1,
                    imagen = item.imagen
                )
            )
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
        if (index in carrito.indices) {
            carrito.removeAt(index)
            _uiState.value = _uiState.value.copy(carrito = carrito)
            actualizarTotales()
        }
    }

    fun actualizarCantidadCarrito(index: Int, nuevaCantidad: Int) {
        if (nuevaCantidad < 1) {
            eliminarDelCarrito(index)
            return
        }
        val carrito = _uiState.value.carrito.toMutableList()
        if (index !in carrito.indices) return
        val item = carrito[index]
        val stock = getStockVenta(item.loteDetalleId)
        when {
            nuevaCantidad > stock -> {
                _uiState.value = _uiState.value.copy(
                    error = "Stock insuficiente para ${item.medicamentoNombre}. Disponible: $stock"
                )
            }
            else -> {
                carrito[index] = item.copy(cantidad = nuevaCantidad)
                _uiState.value = _uiState.value.copy(carrito = carrito)
                actualizarTotales()
            }
        }
    }

    private fun getStockVenta(loteDetalleId: Long): Int =
        _uiState.value.lotes
            .flatMap { it.detalles }
            .find { it.id == loteDetalleId }
            ?.cantidad ?: 0

    // ── Cliente / Receta ─────────────────────────────────────────────────────

    fun seleccionarCliente(cliente: ClienteResponse?) {
        _uiState.value = _uiState.value.copy(
            clienteSeleccionado = cliente,
            tipoVenta = if (cliente != null &&
                !cliente.nombre.contains("consumidor", ignoreCase = true)) "cliente" else "rapida",
            showClienteDialog = false,
            // Limpiar saldo si vuelve a venta rápida
            montoUsadoSaldo = 0.0
        )
        actualizarTotales()
    }

    fun seleccionarReceta(receta: RecetaResponse?) {
        _uiState.value = _uiState.value.copy(recetaSeleccionada = receta, showRecetaDialog = false)
    }

    // ── Montos ───────────────────────────────────────────────────────────────

    fun setMontoEfectivo(monto: Double) {
        _uiState.value = _uiState.value.copy(montoEfectivo = monto.coerceAtLeast(0.0))
        actualizarTotales()
    }

    fun setMontoUsadoSaldo(monto: Double) {
        val saldoMax = _uiState.value.clienteSeleccionado?.saldo ?: 0.0
        _uiState.value = _uiState.value.copy(
            montoUsadoSaldo = monto.coerceIn(0.0, saldoMax)
        )
        actualizarTotales()
    }

    // ── Crear venta ──────────────────────────────────────────────────────────

    fun crearVenta(onSuccess: (VentaResponse) -> Unit) {
        val state = _uiState.value

        // Validación 1: carrito vacío
        if (state.carrito.isEmpty()) {
            _uiState.value = state.copy(error = "El carrito está vacío")
            return
        }

        // Validación 2: requiere receta pero no está adjunta
        if (state.requiereReceta && state.recetaSeleccionada == null) {
            _uiState.value = state.copy(error = "Hay medicamentos que requieren receta médica validada")
            return
        }

        // Validación 3: stock suficiente por ítem
        for (item in state.carrito) {
            val stock = getStockVenta(item.loteDetalleId)
            if (item.cantidad > stock) {
                _uiState.value = state.copy(
                    error = "Stock insuficiente para ${item.medicamentoNombre}. Disponible: $stock"
                )
                return
            }
        }

        // Validación 4: pago suficiente ← FIX PRINCIPAL
        val totalPagado = state.montoEfectivo + state.montoUsadoSaldo
        if (totalPagado < state.total) {
            val faltante = state.total - totalPagado
            _uiState.value = state.copy(
                error = "Pago insuficiente. Faltan C$ ${"%.2f".format(faltante)} para cubrir el total"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
                    generarTicket(venta, state.carrito)
                    // Resetear pantalla y notificar al caller
                    _uiState.value = VentaCreateUiState(
                        medicamentos = state.medicamentos,
                        clientes = state.clientes,
                        lotes = state.lotes,
                        recetasDisponibles = state.recetasDisponibles,
                        clienteSeleccionado = state.clientes.find {
                            it.nombre.contains("consumidor", ignoreCase = true)
                        },
                        ventaExitosa = venta
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

    // ── Totales ──────────────────────────────────────────────────────────────

    private fun actualizarTotales() {
        val state = _uiState.value
        val subtotal = state.carrito.sumOf { it.precioUnitario * it.cantidad }
        val total = subtotal * 1.15
        val totalPagado = state.montoEfectivo + state.montoUsadoSaldo
        val cambio = (totalPagado - total).coerceAtLeast(0.0)
        val pagoSuficiente = totalPagado >= total && total > 0

        val requiereReceta = state.carrito.any { item ->
            val det = state.lotes.flatMap { it.detalles }.find { it.id == item.loteDetalleId }
            val med = det?.let { d -> state.medicamentos.find { it.id == d.medicamentoId } }
            med?.receta == true
        }

        _uiState.value = state.copy(
            subtotal = subtotal,
            total = total,
            cambio = cambio,
            pagoSuficiente = pagoSuficiente,
            requiereReceta = requiereReceta
        )
    }

    // ── Ticket PDF ───────────────────────────────────────────────────────────

    private suspend fun generarTicket(venta: VentaResponse, items: List<CarritoItem>) =
        withContext(Dispatchers.IO) {
            runCatching {
                val context = MyApplication.instance
                val pdf = PdfDocument()
                val pageHeight = 40 + items.size * 12 + 80
                val pageInfo = PdfDocument.PageInfo.Builder(227, pageHeight, 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                val bold = Paint().apply {
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val normal = Paint().apply {
                    textSize = 8f
                    typeface = Typeface.DEFAULT
                }
                val right = Paint().apply {
                    textSize = 8f
                    textAlign = Paint.Align.RIGHT
                }

                var y = 20
                canvas.drawText("FARMACIA SANIDAD", 113f, y.toFloat(), bold)
                y += 14
                bold.textAlign = Paint.Align.LEFT
                bold.textSize = 8f
                canvas.drawText("Factura: #${venta.numeroFactura}", 10f, y.toFloat(), bold)
                y += 10
                canvas.drawText("Cliente: ${venta.clienteNombre ?: "Consumidor Final"}", 10f, y.toFloat(), normal)
                y += 10
                canvas.drawText("--------------------------------", 10f, y.toFloat(), normal)
                y += 10

                for (item in items) {
                    canvas.drawText("${item.medicamentoNombre} x${item.cantidad}", 10f, y.toFloat(), normal)
                    canvas.drawText("C$ ${"%.2f".format(item.precioUnitario * item.cantidad)}", 217f, y.toFloat(), right)
                    y += 10
                }

                canvas.drawText("--------------------------------", 10f, y.toFloat(), normal)
                y += 10
                canvas.drawText("Subtotal:  C$ ${"%.2f".format(venta.total!! / 1.15)}", 10f, y.toFloat(), normal)
                y += 10
                canvas.drawText("IVA (15%): C$ ${"%.2f".format(venta.total!! - venta.total!! / 1.15)}", 10f, y.toFloat(), normal)
                y += 10
                canvas.drawText("TOTAL:     C$ ${"%.2f".format(venta.total)}", 10f, y.toFloat(), bold)
                y += 14
                canvas.drawText("¡Gracias por su compra!", 113f, y.toFloat(),
                    bold.apply { textAlign = Paint.Align.CENTER })

                pdf.finishPage(page)
                val file = File(context.cacheDir, "Venta_${venta.numeroFactura}.pdf")
                FileOutputStream(file).use { pdf.writeTo(it) }
                pdf.close()
            }
        }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    fun setShowClienteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClienteDialog = show)
    }

    fun setShowRecetaDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showRecetaDialog = show)
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            medicamentoRepo: MedicamentoRepository,
            clienteRepo: ClienteRepository,
            loteRepo: LoteRepository,
            recetaRepo: RecetaRepository,
            ventaRepo: VentaRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VentaCreateViewModel(medicamentoRepo, clienteRepo, loteRepo, recetaRepo, ventaRepo) as T
        }
    }
}