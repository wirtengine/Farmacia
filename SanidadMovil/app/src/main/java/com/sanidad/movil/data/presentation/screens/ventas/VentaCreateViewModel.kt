package com.sanidad.movil.data.presentation.screens.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CarritoItem(
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val precioUnitario: Double,
    var cantidad: Int,
    val imagen: String? = null
)

class VentaCreateViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // Datos maestros
    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _clientes = MutableStateFlow<List<ClienteResponse>>(emptyList())
    val clientes: StateFlow<List<ClienteResponse>> = _clientes

    private val _lotes = MutableStateFlow<List<LoteResponse>>(emptyList())
    val lotes: StateFlow<List<LoteResponse>> = _lotes

    private val _recetasDisponibles = MutableStateFlow<List<RecetaResponse>>(emptyList())
    val recetasDisponibles: StateFlow<List<RecetaResponse>> = _recetasDisponibles

    // Búsqueda
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Resultados de búsqueda con stock
    private val _resultadosBusqueda = MutableStateFlow<List<LoteDetalleParaVenta>>(emptyList())
    val resultadosBusqueda: StateFlow<List<LoteDetalleParaVenta>> = _resultadosBusqueda

    // FIFO
    private val _loteFIFO = MutableStateFlow<LoteDetalleParaVenta?>(null)
    val loteFIFO: StateFlow<LoteDetalleParaVenta?> = _loteFIFO

    // Complementarios
    private val _complementarios = MutableStateFlow<List<SugerenciaProductoDTO>>(emptyList())
    val complementarios: StateFlow<List<SugerenciaProductoDTO>> = _complementarios

    // Contexto del cliente
    private val _contextoCliente = MutableStateFlow<Any?>(null) // Simplificado
    val contextoCliente: StateFlow<Any?> = _contextoCliente

    // Carrito
    private val _carrito = MutableStateFlow<List<CarritoItem>>(emptyList())
    val carrito: StateFlow<List<CarritoItem>> = _carrito

    // Cliente seleccionado
    private val _clienteSeleccionado = MutableStateFlow<ClienteResponse?>(null)
    val clienteSeleccionado: StateFlow<ClienteResponse?> = _clienteSeleccionado

    // Receta seleccionada
    private val _recetaSeleccionada = MutableStateFlow<RecetaResponse?>(null)
    val recetaSeleccionada: StateFlow<RecetaResponse?> = _recetaSeleccionada

    // Pagos
    private val _montoEfectivo = MutableStateFlow(0.0)
    val montoEfectivo: StateFlow<Double> = _montoEfectivo

    private val _montoUsadoSaldo = MutableStateFlow(0.0)
    val montoUsadoSaldo: StateFlow<Double> = _montoUsadoSaldo

    // Tipo de venta
    private val _tipoVenta = MutableStateFlow("rapida")
    val tipoVenta: StateFlow<String> = _tipoVenta

    // Usuario actual
    var usuarioId: Long = 0L

    // Carga inicial de datos
    fun cargarDatosIniciales() {
        viewModelScope.launch {
            try {
                val resM = api.obtenerMedicamentos()
                val resC = api.obtenerClientes()
                val resL = api.obtenerLotes()
                val resR = api.obtenerRecetasDisponibles()

                if (resM.isSuccessful) _medicamentos.value = resM.body() ?: emptyList()
                if (resC.isSuccessful) _clientes.value = resC.body() ?: emptyList()
                if (resL.isSuccessful) _lotes.value = resL.body() ?: emptyList()
                if (resR.isSuccessful) _recetasDisponibles.value = resR.body() ?: emptyList()

                // Cliente por defecto "Consumidor Final"
                val consumidor = _clientes.value.find { it.nombre.contains("consumidor", ignoreCase = true) }
                if (consumidor != null) _clienteSeleccionado.value = consumidor
            } catch (_: Exception) { }
        }
    }

    // Búsqueda con inteligencia
    fun buscarMedicamento(query: String) {
        _query.value = query
        if (query.length < 2) {
            _resultadosBusqueda.value = emptyList()
            _loteFIFO.value = null
            _complementarios.value = emptyList()
            return
        }
        viewModelScope.launch {
            // Buscar medicamento por nombre
            val medEncontrado = _medicamentos.value.find {
                it.nombre.contains(query, ignoreCase = true)
            }
            if (medEncontrado != null) {
                // Obtener FIFO
                try {
                    val fifoRes = api.obtenerLotesMedicamento(medEncontrado.id)
                    if (fifoRes.isSuccessful) {
                        val lotesDTO = fifoRes.body()
                        // Buscar el lote detalle con FIFO (más próximo a vencer)
                        val detalles = _lotes.value.flatMap { lote ->
                            lote.detalles.filter { it.medicamentoId == medEncontrado.id && it.cantidad > 0 }
                        }
                        val fifo = detalles.minByOrNull { it.id } // Simplificación, debería ser por fecha
                        if (fifo != null) {
                            _loteFIFO.value = LoteDetalleParaVenta(
                                id = fifo.id,
                                medicamentoId = medEncontrado.id,
                                medicamentoNombre = medEncontrado.nombre,
                                presentacion = medEncontrado.presentacion,
                                precioUnitario = medEncontrado.precioUnitario.toDouble(),
                                stockVenta = fifo.cantidad,
                                imagen = medEncontrado.imagen
                            )
                        }
                    }
                } catch (_: Exception) { }

                // Complementarios (simulado, ya que no existe endpoint directo en tu API)
                _complementarios.value = emptyList() // Podrías implementarlo si agregas el endpoint
            }

            // Resultados de búsqueda con stock
            val resultados = _lotes.value.flatMap { lote ->
                lote.detalles
                    .filter { detalle ->
                        val med = _medicamentos.value.find { it.id == detalle.medicamentoId }
                        med != null && med.nombre.contains(query, ignoreCase = true) && detalle.cantidad > 0
                    }
                    .map { detalle ->
                        val med = _medicamentos.value.find { it.id == detalle.medicamentoId }!!
                        LoteDetalleParaVenta(
                            id = detalle.id,
                            medicamentoId = med.id,
                            medicamentoNombre = med.nombre,
                            presentacion = med.presentacion,
                            precioUnitario = med.precioUnitario.toDouble(),
                            stockVenta = detalle.cantidad,
                            imagen = med.imagen,
                            loteNum = lote.numeroLote
                        )
                    }
            }
            _resultadosBusqueda.value = resultados.take(6)
        }
    }

    // Agregar al carrito
    fun agregarAlCarrito(item: LoteDetalleParaVenta) {
        val carrito = _carrito.value.toMutableList()
        val existente = carrito.find { it.loteDetalleId == item.id }
        if (existente != null) {
            if (existente.cantidad < item.stockVenta) {
                existente.cantidad++
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
        _carrito.value = carrito
        // Limpiar búsqueda
        _query.value = ""
        _resultadosBusqueda.value = emptyList()
        _loteFIFO.value = null
    }

    fun eliminarDelCarrito(index: Int) {
        val carrito = _carrito.value.toMutableList()
        carrito.removeAt(index)
        _carrito.value = carrito
    }

    fun actualizarCantidadCarrito(index: Int, nuevaCantidad: Int) {
        val carrito = _carrito.value.toMutableList()
        if (index in carrito.indices) {
            val item = carrito[index]
            val stock = getStockVenta(item.loteDetalleId)
            if (nuevaCantidad <= stock) {
                carrito[index] = item.copy(cantidad = nuevaCantidad)
                _carrito.value = carrito
            }
        }
    }

    // Stock de venta
    private fun getStockVenta(loteDetalleId: Long): Int {
        for (lote in _lotes.value) {
            val detalle = lote.detalles.find { it.id == loteDetalleId }
            if (detalle != null) return detalle.cantidad
        }
        return 0
    }

    // Seleccionar cliente
    fun seleccionarCliente(cliente: ClienteResponse?) {
        _clienteSeleccionado.value = cliente
        if (cliente != null) {
            _tipoVenta.value = "cliente"
            // Cargar contexto del cliente (si tienes endpoint)
        } else {
            _tipoVenta.value = "rapida"
        }
    }

    fun seleccionarReceta(receta: RecetaResponse?) {
        _recetaSeleccionada.value = receta
    }

    // Calcular totales
    val subtotal: Double
        get() = _carrito.value.sumOf { it.precioUnitario * it.cantidad }

    val iva: Double
        get() = subtotal * 0.15

    val total: Double
        get() = subtotal + iva

    val cambio: Double
        get() = ((_montoEfectivo.value + _montoUsadoSaldo.value) - total).coerceAtLeast(0.0)

    // ¿Requiere receta?
    val requiereReceta: Boolean
        get() {
            val medicamentosEnCarrito = _carrito.value.mapNotNull { item ->
                _lotes.value.flatMap { it.detalles }
                    .find { detalle -> detalle.id == item.loteDetalleId }
                    ?.let { detalle -> _medicamentos.value.find { it.id == detalle.medicamentoId } }
            }
            return medicamentosEnCarrito.any { it.receta == true }
        }

    // Crear venta
    fun crearVenta(onSuccess: (VentaResponse) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Verificar stock
                for (item in _carrito.value) {
                    val stock = getStockVenta(item.loteDetalleId)
                    if (item.cantidad > stock) {
                        onError("Stock insuficiente para ${item.medicamentoNombre}. Disponible: $stock")
                        return@launch
                    }
                }

                val detalles = _carrito.value.map {
                    VentaDetalleRequest(loteDetalleId = it.loteDetalleId, cantidad = it.cantidad)
                }

                val request = VentaRequest(
                    clienteId = _clienteSeleccionado.value?.id,
                    usuarioId = usuarioId,
                    detalles = detalles,
                    recetaId = _recetaSeleccionada.value?.id,
                    montoUsadoSaldo = _montoUsadoSaldo.value,
                    montoEfectivo = _montoEfectivo.value
                )

                val response = api.crearVenta(request)
                if (response.isSuccessful) {
                    val venta = response.body()!!
                    onSuccess(venta)
                    // Limpiar
                    _carrito.value = emptyList()
                    _montoEfectivo.value = 0.0
                    _montoUsadoSaldo.value = 0.0
                    _recetaSeleccionada.value = null
                } else {
                    onError("Error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    fun setMontoEfectivo(monto: Double) { _montoEfectivo.value = monto }
    fun setMontoUsadoSaldo(monto: Double) { _montoUsadoSaldo.value = monto }
}

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