package com.sanidad.movil.presentation.screens.ubicaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ---------- Clases de datos auxiliares ----------
data class Celda(val nivel: Int, val columna: Int, val profundidadIndex: Int)
data class OrigenMovimiento(
    val ubicacionId: Long,
    val loteDetalleId: Long,
    val cantidad: Int,
    val nivel: Int,
    val columna: Int,
    val profundidadIndex: Int,
    val medicamentoNombre: String
)
data class NuevoRackState(
    val nombre: String = "",
    val ancho: Int = 4,
    val alto: Int = 4,
    val profundidad: Int = 2
)
data class LoteConDetalles(val lote: LoteResponse, val detallesDisponibles: List<LoteDetalleResponse>)
data class InfoCeldaOcupada(
    val ubicacionId: Long,
    val medicamentoNombre: String,
    val factura: String?,
    val vence: String?,
    val loteDetalleId: Long
)

data class UbicacionesUiState(
    val racks: List<RackResponse> = emptyList(),
    val lotes: List<LoteResponse> = emptyList(),
    val medicamentos: List<MedicamentoResponse> = emptyList(),
    val todasUbicaciones: List<UbicacionLoteResponse> = emptyList(),
    val rackSeleccionado: RackResponse? = null,
    val ubicacionesRack: List<UbicacionLoteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchTerm: String = "",
    val celdaSeleccionada: Celda? = null,
    val detalleSeleccionado: LoteDetalleResponse? = null,
    val cantidad: Int = 1,
    val modoMovimiento: Boolean = false,
    val origenMovimiento: OrigenMovimiento? = null,
    val showNuevoRackDialog: Boolean = false,
    val nuevoRack: NuevoRackState = NuevoRackState(),
    // paneles de asignación/movimiento visibles
    val showAsignarPanel: Boolean = false,
    val showMoverPanel: Boolean = false
) {
    // ── Info de la celda seleccionada ──
    val infoCelda: InfoCeldaOcupada? get() {
        val celda = celdaSeleccionada ?: return null
        val u = ubicacionesRack.firstOrNull {
            it.nivel == celda.nivel && it.columna == celda.columna && it.profundidadIndex == celda.profundidadIndex
        } ?: return null
        val lote = lotes.firstOrNull { l -> l.detalles.any { d -> d.id == u.loteDetalleId } }
        val det = lote?.detalles?.firstOrNull { it.id == u.loteDetalleId }
        val med = medicamentos.firstOrNull { it.id == det?.medicamentoId }
        return InfoCeldaOcupada(
            ubicacionId = u.id,
            medicamentoNombre = med?.nombre ?: "Desconocido",
            factura = lote?.factura,
            vence = lote?.fechaVencimiento,
            loteDetalleId = u.loteDetalleId
        )
    }

    // ── Lotes con stock real disponible (considera todas las ubicaciones del sistema) ──
    val lotesConStockReal: List<LoteConDetalles> get() {
        val term = searchTerm.lowercase().trim()
        return lotes.filter { lote ->
            lote.activo && lote.detalles.any { det ->
                val yaUbicado = todasUbicaciones.filter { it.loteDetalleId == det.id }.sumOf { it.cantidad }
                det.cantidad - yaUbicado > 0
            }
        }.map { lote ->
            val detallesCalc = lote.detalles.mapNotNull { det ->
                val yaUbicado = todasUbicaciones.filter { it.loteDetalleId == det.id }.sumOf { it.cantidad }
                val stock = det.cantidad - yaUbicado
                if (stock > 0) det.copy(cantidad = stock) else null
            }
            LoteConDetalles(lote = lote, detallesDisponibles = detallesCalc)
        }.filter { lc ->
            if (term.isEmpty()) true
            else {
                lc.lote.factura?.lowercase()?.contains(term) == true ||
                        lc.detallesDisponibles.any { det ->
                            medicamentos.firstOrNull { it.id == det.medicamentoId }?.nombre?.lowercase()?.contains(term) == true
                        }
            }
        }
    }
}

class UbicacionesViewModel(
    private val rackRepo: RackRepository,
    private val ubicacionRepo: UbicacionRepository,
    private val loteRepo: LoteRepository,
    private val medicamentoRepo: MedicamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UbicacionesUiState())
    val uiState: StateFlow<UbicacionesUiState> = _uiState

    init { cargarDatosBase() }

    // ── CARGA INICIAL ──
    fun cargarDatosBase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val racksRes = rackRepo.getRacks()
            val lotesRes = loteRepo.getLotes()
            val medsRes = medicamentoRepo.getMedicamentos()
            val todasUbiRes = ubicacionRepo.getTodasUbicaciones()

            val racks = (racksRes as? ApiResult.Success)?.data ?: emptyList()
            val lotes = (lotesRes as? ApiResult.Success)?.data ?: emptyList()
            val meds = (medsRes as? ApiResult.Success)?.data ?: emptyList()
            val todasUbi = (todasUbiRes as? ApiResult.Success)?.data ?: emptyList()

            _uiState.value = _uiState.value.copy(
                racks = racks,
                lotes = lotes,
                medicamentos = meds,
                todasUbicaciones = todasUbi,
                isLoading = false
            )
        }
    }

    // ── SELECCIONAR RACK ──
    fun seleccionarRack(rackId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rackRes = rackRepo.getRack(rackId)
            val ubicRes = ubicacionRepo.getUbicacionesPorRack(rackId)
            val rack = (rackRes as? ApiResult.Success)?.data
            val ubicaciones = (ubicRes as? ApiResult.Success)?.data ?: emptyList()
            _uiState.value = _uiState.value.copy(
                rackSeleccionado = rack,
                ubicacionesRack = ubicaciones,
                isLoading = false
            )
            resetearSeleccion()
        }
    }

    // ── CLICK EN CELDA ──
    fun onCeldaClick(nivel: Int, columna: Int, profundidadIndex: Int) {
        val state = _uiState.value
        val ocupada = state.ubicacionesRack.firstOrNull {
            it.nivel == nivel && it.columna == columna && it.profundidadIndex == profundidadIndex
        }

        // MODO MOVIMIENTO: intentar mover
        if (state.modoMovimiento && state.origenMovimiento != null) {
            if (ocupada != null) {
                _uiState.value = state.copy(error = "La celda destino está ocupada")
                return
            }
            realizarMovimiento(
                origen = state.origenMovimiento!!,
                destinoNivel = nivel,
                destinoCol = columna,
                destinoProf = profundidadIndex
            )
            return
        }

        // MODO NORMAL
        if (ocupada != null) {
            // Celda ocupada → abrir panel de mover (o iniciar modo movimiento)
            _uiState.value = state.copy(
                modoMovimiento = true,
                origenMovimiento = OrigenMovimiento(
                    ubicacionId = ocupada.id,
                    loteDetalleId = ocupada.loteDetalleId,
                    cantidad = ocupada.cantidad,
                    nivel = nivel,
                    columna = columna,
                    profundidadIndex = profundidadIndex,
                    medicamentoNombre = obtenerNombreMedicamento(ocupada.loteDetalleId)
                ),
                celdaSeleccionada = null,
                showAsignarPanel = false
            )
        } else {
            // Celda libre → abrir panel de asignación
            _uiState.value = state.copy(
                celdaSeleccionada = Celda(nivel, columna, profundidadIndex),
                detalleSeleccionado = null,
                cantidad = 1,
                modoMovimiento = false,
                origenMovimiento = null,
                showAsignarPanel = true
            )
        }
    }

    private fun obtenerNombreMedicamento(loteDetalleId: Long): String {
        val state = _uiState.value
        val lote = state.lotes.firstOrNull { it.detalles.any { d -> d.id == loteDetalleId } }
        val det = lote?.detalles?.firstOrNull { it.id == loteDetalleId }
        val med = state.medicamentos.firstOrNull { it.id == det?.medicamentoId }
        return med?.nombre ?: "Producto"
    }

    private fun realizarMovimiento(origen: OrigenMovimiento, destinoNivel: Int, destinoCol: Int, destinoProf: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rackId = _uiState.value.rackSeleccionado?.id ?: return@launch

            // 1. Eliminar origen
            val delRes = ubicacionRepo.eliminarUbicacion(origen.ubicacionId)
            if (delRes is ApiResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = delRes.message)
                return@launch
            }

            // 2. Asignar destino
            val request = UbicacionLoteRequest(
                loteDetalleId = origen.loteDetalleId,
                rackId = rackId,
                nivel = destinoNivel,
                columna = destinoCol,
                profundidadIndex = destinoProf,
                cantidad = origen.cantidad
            )
            val asignRes = ubicacionRepo.asignarUbicacion(request)
            if (asignRes is ApiResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = asignRes.message)
                return@launch
            }

            // 3. Recargar
            cargarDatosBase()
            seleccionarRack(rackId)
            resetearSeleccion()
        }
    }

    fun cancelarMovimiento() {
        _uiState.value = _uiState.value.copy(modoMovimiento = false, origenMovimiento = null)
    }

    // ── ASIGNAR EN CASCADA ──
    fun asignarEnCascada() {
        val state = _uiState.value
        val celda = state.celdaSeleccionada ?: return
        val detalle = state.detalleSeleccionado ?: return
        val rack = state.rackSeleccionado ?: return
        val cantidad = state.cantidad

        val stockDisponible = detalle.cantidad
        if (cantidad > stockDisponible) {
            _uiState.value = state.copy(error = "Stock insuficiente. Solo hay $stockDisponible unidades.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var restantes = cantidad
            var n = celda.nivel
            var c = celda.columna
            var p = celda.profundidadIndex
            val maxIntentos = rack.alto * rack.ancho * rack.profundidad
            var intentos = 0
            var asignadas = 0

            while (restantes > 0 && intentos < maxIntentos) {
                val ocupada = state.ubicacionesRack.any {
                    it.nivel == n && it.columna == c && it.profundidadIndex == p && it.activo
                }
                if (!ocupada) {
                    val request = UbicacionLoteRequest(
                        loteDetalleId = detalle.id,
                        rackId = rack.id,
                        nivel = n,
                        columna = c,
                        profundidadIndex = p,
                        cantidad = 1
                    )
                    when (val res = ubicacionRepo.asignarUbicacion(request)) {
                        is ApiResult.Success -> asignadas++
                        is ApiResult.Error -> {} // continuar
                        else -> {}
                    }
                    restantes--
                }
                p++
                if (p >= rack.profundidad) { p = 0; c++ }
                if (c >= rack.ancho) { c = 0; n++ }
                if (n >= rack.alto) break
                intentos++
            }

            cargarDatosBase()
            seleccionarRack(rack.id)
            resetearSeleccion()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (restantes > 0) "Faltaron $restantes unidades por ubicar." else null
            )
        }
    }

    // ── ELIMINAR UBICACIÓN ──
    fun eliminarUbicacion(ubicacionId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = ubicacionRepo.eliminarUbicacion(ubicacionId)) {
                is ApiResult.Success -> {
                    cargarDatosBase()
                    _uiState.value.rackSeleccionado?.let { seleccionarRack(it.id) }
                    resetearSeleccion()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // ── NUEVO RACK ──
    fun actualizarNuevoRack(campo: String, valor: Any) {
        val r = _uiState.value.nuevoRack
        _uiState.value = _uiState.value.copy(nuevoRack = when (campo) {
            "nombre" -> r.copy(nombre = valor as String)
            "ancho" -> r.copy(ancho = valor as Int)
            "alto" -> r.copy(alto = valor as Int)
            "profundidad" -> r.copy(profundidad = valor as Int)
            else -> r
        })
    }

    fun crearRack() {
        viewModelScope.launch {
            val r = _uiState.value.nuevoRack
            _uiState.value = _uiState.value.copy(isLoading = true)
            val request = RackRequest(
                nombre = r.nombre,
                ancho = r.ancho,
                alto = r.alto,
                profundidad = r.profundidad,
                descripcion = null
            )
            when (val res = rackRepo.crearRack(request)) {
                is ApiResult.Success -> {
                    cargarDatosBase()
                    _uiState.value = _uiState.value.copy(
                        showNuevoRackDialog = false,
                        nuevoRack = NuevoRackState(),
                        isLoading = false
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun eliminarRack(rackId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = rackRepo.eliminarRack(rackId)) {
                is ApiResult.Success -> {
                    if (_uiState.value.rackSeleccionado?.id == rackId) {
                        _uiState.value = _uiState.value.copy(rackSeleccionado = null, ubicacionesRack = emptyList())
                    }
                    cargarDatosBase()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // ── MÉTODOS PÚBLICOS SIMPLES ──
    fun setSearchTerm(term: String) { _uiState.value = _uiState.value.copy(searchTerm = term) }
    fun abrirNuevoRackDialog() { _uiState.value = _uiState.value.copy(showNuevoRackDialog = true) }
    fun cerrarNuevoRackDialog() { _uiState.value = _uiState.value.copy(showNuevoRackDialog = false) }
    fun setDetalleSeleccionado(det: LoteDetalleResponse?) { _uiState.value = _uiState.value.copy(detalleSeleccionado = det) }
    fun setCantidad(cant: Int) { _uiState.value = _uiState.value.copy(cantidad = cant) }
    fun cerrarPanelAsignar() { _uiState.value = _uiState.value.copy(showAsignarPanel = false, celdaSeleccionada = null) }
    fun limpiarError() { _uiState.value = _uiState.value.copy(error = null) }

    fun resetearSeleccion() {
        _uiState.value = _uiState.value.copy(
            celdaSeleccionada = null,
            detalleSeleccionado = null,
            cantidad = 1,
            searchTerm = "",
            modoMovimiento = false,
            origenMovimiento = null,
            showAsignarPanel = false,
            showMoverPanel = false
        )
    }
}