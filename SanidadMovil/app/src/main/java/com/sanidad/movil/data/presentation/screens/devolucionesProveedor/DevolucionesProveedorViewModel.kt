package com.sanidad.movil.data.presentation.screens.devolucionesProveedor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ItemDevolucionProv(
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val cantidadDisponible: Int,
    var cantidadDevuelta: Int = 0,
    val imagen: String? = null
)

class DevolucionesProveedorViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS MAESTROS ======================
    private val _devoluciones = MutableStateFlow<List<DevolucionProveedorResponse>>(emptyList())
    val devoluciones: StateFlow<List<DevolucionProveedorResponse>> = _devoluciones

    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _proveedores = MutableStateFlow<List<ProveedorResponse>>(emptyList())
    val proveedores: StateFlow<List<ProveedorResponse>> = _proveedores

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

    private val _busquedaLote = MutableStateFlow("")
    val busquedaLote: StateFlow<String> = _busquedaLote

    private val _loteSeleccionado = MutableStateFlow<LoteResponse?>(null)
    val loteSeleccionado: StateFlow<LoteResponse?> = _loteSeleccionado

    private val _itemsDevolucion = MutableStateFlow<List<ItemDevolucionProv>>(emptyList())
    val itemsDevolucion: StateFlow<List<ItemDevolucionProv>> = _itemsDevolucion

    private val _motivo = MutableStateFlow("")
    val motivo: StateFlow<String> = _motivo

    // ====================== FILTRADO ======================
    val devolucionesFiltradas: List<DevolucionProveedorResponse>
        get() {
            val term = _searchTerm.value.lowercase().trim()
            val estado = _estadoFiltro.value
            return _devoluciones.value.filter { d ->
                val matchSearch = term.isEmpty() ||
                        d.numeroDevolucion?.lowercase()?.contains(term) == true ||
                        d.numeroFacturaLote?.lowercase()?.contains(term) == true ||
                        d.proveedorNombre?.lowercase()?.contains(term) == true
                val matchEstado = estado == "TODOS" || d.estado == estado
                matchSearch && matchEstado
            }
        }

    val totalPages: Int
        get() {
            val total = devolucionesFiltradas.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedDevoluciones: List<DevolucionProveedorResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, devolucionesFiltradas.size)
            if (start >= end) return emptyList()
            return devolucionesFiltradas.subList(start, end)
        }

    // ====================== LOTES FILTRADOS PARA EL DRAWER ======================
    val lotesFiltrados: List<LoteResponse>
        get() {
            val term = _busquedaLote.value.lowercase().trim()
            return _lotes.value.filter { l ->
                l.factura?.lowercase()?.contains(term) == true
            }
        }

    // ====================== CARGAR DATOS INICIALES ======================
    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val devRes = api.obtenerDevolucionesProveedor()
                val medRes = api.obtenerMedicamentos()
                val provRes = api.obtenerProveedores()
                val lotRes = api.obtenerLotes()
                _devoluciones.value = devRes.body()?.sortedByDescending { it.id } ?: emptyList()
                _medicamentos.value = medRes.body() ?: emptyList()
                _proveedores.value = provRes.body() ?: emptyList()
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
        _loteSeleccionado.value = null
        _itemsDevolucion.value = emptyList()
        _motivo.value = ""
        _busquedaLote.value = ""
        viewModelScope.launch {
            try {
                val lotRes = api.obtenerLotes()
                if (lotRes.isSuccessful) _lotes.value = lotRes.body() ?: emptyList()
            } catch (_: Exception) { }
        }
        _showDrawer.value = true
    }

    fun cerrarDrawer() {
        _showDrawer.value = false
    }

    fun setBusquedaLote(term: String) {
        _busquedaLote.value = term
    }

    fun seleccionarLote(lote: LoteResponse) {
        viewModelScope.launch {
            try {
                val res = api.obtenerLote(lote.id)
                if (res.isSuccessful) {
                    val detalles = res.body()!!.detalles.map { det ->
                        val med = _medicamentos.value.find { it.id == det.medicamentoId }
                        ItemDevolucionProv(
                            loteDetalleId = det.id,
                            medicamentoNombre = med?.nombre ?: det.medicamentoNombre,
                            cantidadDisponible = det.cantidad,
                            cantidadDevuelta = 0,
                            imagen = med?.imagen
                        )
                    }
                    _itemsDevolucion.value = detalles
                    _loteSeleccionado.value = lote
                }
            } catch (_: Exception) { }
        }
    }

    fun actualizarCantidad(loteDetalleId: Long, cantidad: Int) {
        _itemsDevolucion.value = _itemsDevolucion.value.map {
            if (it.loteDetalleId == loteDetalleId) it.copy(cantidadDevuelta = cantidad.coerceIn(0, it.cantidadDisponible))
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
                .map { DevolucionProveedorDetalleRequest(it.loteDetalleId, it.cantidadDevuelta) }

            if (detalles.isEmpty()) {
                onError("Seleccione cantidades válidas")
                return@launch
            }

            try {
                val request = DevolucionProveedorRequest(
                    loteId = _loteSeleccionado.value!!.id,
                    solicitadoPorId = usuarioId,
                    motivo = _motivo.value.ifBlank { null },
                    detalles = detalles
                )
                val res = api.solicitarDevolucionProveedor(request)
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
    fun aprobarDevolucion(id: Long, aprobadoPorId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = DevolucionProveedorAprobarRequest(
                    devolucionId = id,
                    aprobadoPorId = aprobadoPorId,
                    aprobada = true,
                    motivoRechazo = null
                )
                api.aprobarDevolucionProveedor(request)
                cargarDatos()
                onSuccess()
            } catch (_: Exception) { }
        }
    }

    fun rechazarDevolucion(id: Long, aprobadoPorId: Long, motivoRechazo: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = DevolucionProveedorAprobarRequest(
                    devolucionId = id,
                    aprobadoPorId = aprobadoPorId,
                    aprobada = false,
                    motivoRechazo = motivoRechazo
                )
                api.aprobarDevolucionProveedor(request)
                cargarDatos()
                onSuccess()
            } catch (_: Exception) { }
        }
    }

    fun obtenerMedicamentoDesdeDetalle(det: DevolucionProveedorDetalleResponse): MedicamentoResponse? {
        // Buscar el medicamento asociado al loteDetalleId
        val loteDet = _lotes.value.flatMap { it.detalles }.find { it.id == det.loteDetalleId }
        if (loteDet != null) {
            return _medicamentos.value.find { it.id == loteDet.medicamentoId }
        }
        return null
    }
}