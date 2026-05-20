package com.sanidad.movil.data.presentation.screens.devoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ItemDevolucion(
    val ventaDetalleId: Long,
    val producto: String,
    val cantidadMax: Int,
    var cantidadDevuelta: Int = 0,
    val precioUnitario: Double,
    val imagen: String? = null
)

class DevolucionesViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS MAESTROS ======================
    private val _devoluciones = MutableStateFlow<List<DevolucionResponse>>(emptyList())
    val devoluciones: StateFlow<List<DevolucionResponse>> = _devoluciones

    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _lotes = MutableStateFlow<List<LoteResponse>>(emptyList())
    val lotes: StateFlow<List<LoteResponse>> = _lotes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ====================== BÚSQUEDA Y FILTROS ======================
    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm

    private val _estadoFiltro = MutableStateFlow("TODOS")
    val estadoFiltro: StateFlow<String> = _estadoFiltro

    // ====================== PAGINACIÓN ======================
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage
    val rowsPerPage = 15

    // ====================== DRAWER NUEVA DEVOLUCIÓN ======================
    private val _showDrawer = MutableStateFlow(false)
    val showDrawer: StateFlow<Boolean> = _showDrawer

    private val _ventas = MutableStateFlow<List<VentaResponse>>(emptyList())
    val ventas: StateFlow<List<VentaResponse>> = _ventas

    private val _busquedaVenta = MutableStateFlow("")
    val busquedaVenta: StateFlow<String> = _busquedaVenta

    private val _ventaSeleccionada = MutableStateFlow<VentaResponse?>(null)
    val ventaSeleccionada: StateFlow<VentaResponse?> = _ventaSeleccionada

    private val _itemsDevolucion = MutableStateFlow<List<ItemDevolucion>>(emptyList())
    val itemsDevolucion: StateFlow<List<ItemDevolucion>> = _itemsDevolucion

    private val _motivo = MutableStateFlow("")
    val motivo: StateFlow<String> = _motivo

    // ====================== FILTRADO ======================
    val devolucionesFiltradas: List<DevolucionResponse>
        get() {
            val term = _searchTerm.value.lowercase().trim()
            val estado = _estadoFiltro.value
            return _devoluciones.value.filter { d ->
                val matchSearch = term.isEmpty() ||
                        d.numeroDevolucion?.lowercase()?.contains(term) == true ||
                        d.numeroFactura.lowercase().contains(term)
                val matchEstado = estado == "TODOS" || d.estado == estado
                matchSearch && matchEstado
            }
        }

    val totalPages: Int
        get() {
            val total = devolucionesFiltradas.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedDevoluciones: List<DevolucionResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, devolucionesFiltradas.size)
            if (start >= end) return emptyList()
            return devolucionesFiltradas.subList(start, end)
        }

    // ====================== VENTAS FILTRADAS PARA EL DRAWER ======================
    val ventasFiltradas: List<VentaResponse>
        get() {
            val term = _busquedaVenta.value.lowercase().trim()
            return _ventas.value.filter {
                it.numeroFactura.lowercase().contains(term)
            }
        }

    // ====================== CARGAR DATOS INICIALES ======================
    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val devRes = api.obtenerDevoluciones()
                val medRes = api.obtenerMedicamentos()
                val lotRes = api.obtenerLotes()
                _devoluciones.value = devRes.body()?.sortedByDescending { it.id } ?: emptyList()
                _medicamentos.value = medRes.body() ?: emptyList()
                _lotes.value = lotRes.body() ?: emptyList()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearch(term: String) {
        _searchTerm.value = term
        _currentPage.value = 1
    }

    fun setEstadoFiltro(estado: String) {
        _estadoFiltro.value = estado
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        if (page in 1..totalPages) _currentPage.value = page
    }

    // ====================== ABRIR DRAWER PARA NUEVA DEVOLUCIÓN ======================
    fun abrirNuevaDevolucion() {
        _ventaSeleccionada.value = null
        _itemsDevolucion.value = emptyList()
        _motivo.value = ""
        _busquedaVenta.value = ""
        viewModelScope.launch {
            try {
                val res = api.obtenerVentas()
                if (res.isSuccessful) {
                    _ventas.value = res.body() ?: emptyList()
                }
            } catch (_: Exception) { }
        }
        _showDrawer.value = true
    }

    fun cerrarDrawer() {
        _showDrawer.value = false
    }

    fun setBusquedaVenta(term: String) {
        _busquedaVenta.value = term
    }

    fun seleccionarVenta(venta: VentaResponse) {
        viewModelScope.launch {
            try {
                val res = api.obtenerVenta(venta.id)
                if (res.isSuccessful) {
                    val detalles = res.body()!!.detalles.map { d ->
                        val med = _medicamentos.value.find { it.id == d.loteDetalleId?.let {
                            _lotes.value.flatMap { l -> l.detalles }.find { det -> det.id == it }?.medicamentoId
                        }}
                        ItemDevolucion(
                            ventaDetalleId = d.id,
                            producto = d.medicamentoNombre,
                            cantidadMax = d.cantidad,
                            cantidadDevuelta = 0,
                            precioUnitario = d.precioUnitario,
                            imagen = med?.imagen
                        )
                    }
                    _itemsDevolucion.value = detalles
                    _ventaSeleccionada.value = res.body()
                }
            } catch (_: Exception) { }
        }
    }

    fun actualizarCantidad(ventaDetalleId: Long, cantidad: Int) {
        _itemsDevolucion.value = _itemsDevolucion.value.map {
            if (it.ventaDetalleId == ventaDetalleId) it.copy(cantidadDevuelta = cantidad.coerceIn(0, it.cantidadMax))
            else it
        }
    }

    fun setMotivo(motivo: String) {
        _motivo.value = motivo
    }

    // ====================== SOLICITAR DEVOLUCIÓN ======================
    fun solicitarDevolucion(usuarioId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val detalles = _itemsDevolucion.value
                .filter { it.cantidadDevuelta > 0 }
                .map { DevolucionDetalleRequest(it.ventaDetalleId, it.cantidadDevuelta) }

            if (detalles.isEmpty() || _motivo.value.isBlank()) {
                onError("Complete todos los datos")
                return@launch
            }

            try {
                val request = DevolucionRequest(
                    ventaId = _ventaSeleccionada.value!!.id,
                    solicitadoPorId = usuarioId,
                    motivo = _motivo.value,
                    detalles = detalles
                )
                val res = api.solicitarDevolucion(request)
                if (res.isSuccessful) {
                    _showDrawer.value = false
                    cargarDatos()
                    onSuccess()
                } else {
                    onError("Error ${res.code()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    // ====================== APROBAR / RECHAZAR ======================
    fun aprobarDevolucion(devolucionId: Long, aprobada: Boolean, aprobadoPorId: Long, motivoRechazo: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = DevolucionAprobarRequest(
                    devolucionId = devolucionId,
                    aprobadoPorId = aprobadoPorId,
                    aprobada = aprobada,
                    motivoRechazo = if (aprobada) null else motivoRechazo
                )
                val res = api.aprobarDevolucion(request)
                if (res.isSuccessful) {
                    cargarDatos()
                    onSuccess()
                }
            } catch (_: Exception) { }
        }
    }

    // ====================== OBTENER MEDICAMENTO DESDE DETALLE ======================
    fun obtenerMedicamentoDesdeDetalle(det: DevolucionDetalleResponse): MedicamentoResponse? {
        if (det.loteDetalleId != null) {
            val loteDet = _lotes.value.flatMap { it.detalles }.find { it.id == det.loteDetalleId }
            if (loteDet != null) {
                return _medicamentos.value.find { it.id == loteDet.medicamentoId }
            }
        }
        return null
    }
}