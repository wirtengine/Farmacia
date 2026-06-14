package com.sanidad.movil.presentation.screens.devoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.DevolucionRepository
import com.sanidad.movil.data.repository.MedicamentoRepository
import com.sanidad.movil.data.repository.LoteRepository
import com.sanidad.movil.data.repository.VentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ItemDevolucion(
    val ventaDetalleId: Long,
    val producto: String,
    val cantidadMax: Int,
    val cantidadDevuelta: Int = 0,
    val precioUnitario: Double,
    val imagen: String? = null
)

data class DevolucionesUiState(
    val devoluciones: List<DevolucionResponse> = emptyList(),
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val lotes: List<LoteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val estadoFiltro: String = "TODOS",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val showDrawer: Boolean = false,
    val ventas: List<VentaResponse> = emptyList(),
    val busquedaVenta: String = "",
    val ventaSeleccionada: VentaResponse? = null,
    val itemsDevolucion: List<ItemDevolucion> = emptyList(),
    val motivo: String = "",

    val showAprobarDialog: Boolean = false,
    val devolucionAprobarId: Long? = null,
    val aprobarAccion: Boolean = true,
    val showMotivoRechazoDialog: Boolean = false,
    val motivoRechazo: String = ""
)

class DevolucionesViewModel(
    private val devolucionRepo: DevolucionRepository,
    private val medicamentoRepo: MedicamentoRepository,
    private val loteRepo: LoteRepository,
    private val ventaRepo: VentaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionesUiState())
    val uiState: StateFlow<DevolucionesUiState> = _uiState

    private val rowsPerPage = 15

    init { cargarDatos() }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val devResult = devolucionRepo.getDevoluciones()
            val medResult = medicamentoRepo.getMedicamentos()
            val lotResult = loteRepo.getLotes()

            val devoluciones = when (devResult) {
                is ApiResult.Success -> devResult.data.sortedByDescending { it.id }
                else -> emptyList()
            }
            val medicamentos = when (medResult) {
                is ApiResult.Success -> medResult.data
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
                lotes = lotes,
                isLoading = false,
                totalPages = calcularTotalPaginas(filtered.size),
                currentPage = 1
            )
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
            ventaSeleccionada = null,
            itemsDevolucion = emptyList(),
            motivo = "",
            busquedaVenta = ""
        )
        viewModelScope.launch {
            when (val res = ventaRepo.getVentas()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(ventas = res.data)
                else -> {}
            }
        }
    }

    fun cerrarDrawer() {
        _uiState.value = _uiState.value.copy(showDrawer = false)
    }

    fun setBusquedaVenta(term: String) {
        _uiState.value = _uiState.value.copy(busquedaVenta = term)
    }

    fun seleccionarVenta(venta: VentaResponse) {
        viewModelScope.launch {
            when (val res = ventaRepo.getVenta(venta.id)) {
                is ApiResult.Success -> {
                    val ventaConDetalles = res.data
                    val detalles = ventaConDetalles.detalles.map { d ->
                        val med = _uiState.value.medicamentos.find { it.nombre == d.medicamentoNombre }
                        ItemDevolucion(
                            ventaDetalleId = d.id,
                            producto = d.medicamentoNombre,
                            cantidadMax = d.cantidad,
                            cantidadDevuelta = 0,
                            precioUnitario = d.precioUnitario,
                            imagen = med?.imagen
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        ventaSeleccionada = ventaConDetalles,
                        itemsDevolucion = detalles
                    )
                }
                else -> {}
            }
        }
    }

    fun actualizarCantidad(ventaDetalleId: Long, cantidad: Int) {
        val nuevosItems = _uiState.value.itemsDevolucion.map {
            if (it.ventaDetalleId == ventaDetalleId) it.copy(cantidadDevuelta = cantidad.coerceIn(0, it.cantidadMax))
            else it
        }
        _uiState.value = _uiState.value.copy(itemsDevolucion = nuevosItems)
    }

    fun setMotivo(motivo: String) {
        _uiState.value = _uiState.value.copy(motivo = motivo)
    }

    fun solicitarDevolucion(usuarioId: Long) {
        viewModelScope.launch {
            val detalles = _uiState.value.itemsDevolucion
                .filter { it.cantidadDevuelta > 0 }
                .map { DevolucionDetalleRequest(it.ventaDetalleId, it.cantidadDevuelta) }

            if (detalles.isEmpty() || _uiState.value.motivo.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Complete todos los datos")
                return@launch
            }

            val request = DevolucionRequest(
                ventaId = _uiState.value.ventaSeleccionada!!.id,
                solicitadoPorId = usuarioId,
                motivo = _uiState.value.motivo,
                detalles = detalles
            )
            when (val res = devolucionRepo.solicitarDevolucion(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showDrawer = false)
                    cargarDatos()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                else -> {}
            }
        }
    }

    fun mostrarAprobarDialog(id: Long, aprobar: Boolean) {
        _uiState.value = _uiState.value.copy(
            showAprobarDialog = true,
            devolucionAprobarId = id,
            aprobarAccion = aprobar
        )
    }

    fun ocultarAprobarDialog() {
        _uiState.value = _uiState.value.copy(showAprobarDialog = false)
    }

    fun mostrarMotivoRechazoDialog(id: Long) {
        _uiState.value = _uiState.value.copy(
            showMotivoRechazoDialog = true,
            devolucionAprobarId = id
        )
    }

    fun ocultarMotivoRechazoDialog() {
        _uiState.value = _uiState.value.copy(showMotivoRechazoDialog = false, motivoRechazo = "")
    }

    fun setMotivoRechazo(motivo: String) {
        _uiState.value = _uiState.value.copy(motivoRechazo = motivo)
    }

    fun confirmarAprobar(usuarioId: Long) {
        val id = _uiState.value.devolucionAprobarId ?: return
        viewModelScope.launch {
            val request = DevolucionAprobarRequest(
                devolucionId = id,
                aprobadoPorId = usuarioId,
                aprobada = _uiState.value.aprobarAccion,
                motivoRechazo = if (_uiState.value.aprobarAccion) null else _uiState.value.motivoRechazo
            )
            when (val res = devolucionRepo.aprobarDevolucion(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showAprobarDialog = false,
                        showMotivoRechazoDialog = false
                    )
                    cargarDatos()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = res.message)
                else -> {}
            }
        }
    }

    // ---- Funciones auxiliares ----

    private fun filtrarDevoluciones(lista: List<DevolucionResponse>, term: String, estado: String): List<DevolucionResponse> {
        val lower = term.lowercase().trim()
        return lista.filter { d ->
            val matchSearch = lower.isEmpty() ||
                    d.numeroDevolucion?.lowercase()?.contains(lower) == true ||
                    d.numeroFactura.lowercase().contains(lower)
            val matchEstado = estado == "TODOS" || d.estado == estado
            matchSearch && matchEstado
        }
    }

    private fun calcularTotalPaginas(total: Int) = if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage

    val devolucionesFiltradas: List<DevolucionResponse>
        get() = filtrarDevoluciones(_uiState.value.devoluciones, _uiState.value.searchTerm, _uiState.value.estadoFiltro)

    val paginatedDevoluciones: List<DevolucionResponse>
        get() {
            val start = (_uiState.value.currentPage - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, devolucionesFiltradas.size)
            if (start >= end) return emptyList()
            return devolucionesFiltradas.subList(start, end)
        }

    val ventasFiltradas: List<VentaResponse>
        get() {
            val term = _uiState.value.busquedaVenta.lowercase().trim()
            return _uiState.value.ventas.filter { it.numeroFactura.lowercase().contains(term) }
        }

    fun obtenerMedicamentoDesdeDetalle(det: DevolucionDetalleResponse): MedicamentoResponse? {
        val lotes = _uiState.value.lotes
        val medicamentos = _uiState.value.medicamentos

        // 1. Buscar por loteDetalleId si existe
        if (det.loteDetalleId != null) {
            val loteDet = lotes.flatMap { it.detalles }.find { it.id == det.loteDetalleId }
            if (loteDet != null) {
                return medicamentos.find { it.id == loteDet.medicamentoId }
            }
        }

        // 2. Buscar por nombre del medicamento (campo medicamentoNombre)
        return medicamentos.find { it.nombre.equals(det.medicamentoNombre, ignoreCase = true) }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun actualizarPaginacion() {
        val total = devolucionesFiltradas.size
        _uiState.value = _uiState.value.copy(totalPages = calcularTotalPaginas(total))
    }
}