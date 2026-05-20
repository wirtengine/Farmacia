package com.sanidad.movil.data.presentation.screens.ubicaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UbicacionesViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // ====================== DATOS MAESTROS ======================
    private val _racks = MutableStateFlow<List<RackResponse>>(emptyList())
    val racks: StateFlow<List<RackResponse>> = _racks

    private val _lotes = MutableStateFlow<List<LoteResponse>>(emptyList())
    val lotes: StateFlow<List<LoteResponse>> = _lotes

    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _todasUbicaciones = MutableStateFlow<List<UbicacionLoteResponse>>(emptyList())
    val todasUbicaciones: StateFlow<List<UbicacionLoteResponse>> = _todasUbicaciones

    // ====================== RACK SELECCIONADO ======================
    private val _rackSeleccionado = MutableStateFlow<RackResponse?>(null)
    val rackSeleccionado: StateFlow<RackResponse?> = _rackSeleccionado

    private val _ubicacionesRack = MutableStateFlow<List<UbicacionLoteResponse>>(emptyList())
    val ubicacionesRack: StateFlow<List<UbicacionLoteResponse>> = _ubicacionesRack

    // ====================== ESTADOS DE UI ======================
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm

    private val _celdaSeleccionada = MutableStateFlow<Celda?>(null)
    val celdaSeleccionada: StateFlow<Celda?> = _celdaSeleccionada

    private val _detalleSeleccionado = MutableStateFlow<LoteDetalleResponse?>(null)
    val detalleSeleccionado: StateFlow<LoteDetalleResponse?> = _detalleSeleccionado

    private val _cantidad = MutableStateFlow(1)
    val cantidad: StateFlow<Int> = _cantidad

    private val _modoMovimiento = MutableStateFlow(false)
    val modoMovimiento: StateFlow<Boolean> = _modoMovimiento

    private val _origenMovimiento = MutableStateFlow<OrigenMovimiento?>(null)
    val origenMovimiento: StateFlow<OrigenMovimiento?> = _origenMovimiento

    // ====================== FORMULARIO NUEVO RACK ======================
    private val _showNuevoRackDialog = MutableStateFlow(false)
    val showNuevoRackDialog: StateFlow<Boolean> = _showNuevoRackDialog

    private val _nuevoRack = MutableStateFlow(NuevoRackState())
    val nuevoRack: StateFlow<NuevoRackState> = _nuevoRack

    // ====================== INFO CELDA OCUPADA ======================
    val infoCelda: StateFlow<InfoCeldaOcupada?> = combine(
        _celdaSeleccionada,
        _ubicacionesRack,
        _lotes,
        _medicamentos
    ) { celda: Celda?,
        ubicaciones: List<UbicacionLoteResponse>,
        lotes: List<LoteResponse>,
        medicamentos: List<MedicamentoResponse> ->
        if (celda == null) {
            null
        } else {
            val u = ubicaciones.firstOrNull {
                it.nivel == celda.nivel && it.columna == celda.columna && it.profundidadIndex == celda.profundidadIndex
            }
            if (u != null) {
                val lote = lotes.firstOrNull { l -> l.detalles.any { d -> d.id == u.loteDetalleId } }
                val det = lote?.detalles?.firstOrNull { it.id == u.loteDetalleId }
                val med = medicamentos.firstOrNull { it.id == det?.medicamentoId }
                InfoCeldaOcupada(
                    ubicacionId = u.id,
                    medicamentoNombre = med?.nombre ?: "Desconocido",
                    factura = lote?.factura,
                    vence = lote?.fechaVencimiento,
                    loteDetalleId = u.loteDetalleId
                )
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ====================== LOTES CON STOCK REAL ======================
    val lotesConStockReal: StateFlow<List<LoteConDetalles>> = combine(
        _lotes,
        _todasUbicaciones,
        _medicamentos,
        _searchTerm
    ) { lotes: List<LoteResponse>,
        todasUbi: List<UbicacionLoteResponse>,
        medicamentos: List<MedicamentoResponse>,
        search: String ->
        lotes.filter { lote ->
            lote.activo && lote.detalles.any { det ->
                val yaUbicado = todasUbi.filter { it.loteDetalleId == det.id }.sumOf { it.cantidad }
                val stock = det.cantidad - yaUbicado
                stock > 0
            }
        }.map { lote ->
            val detallesCalc = lote.detalles.mapNotNull { det ->
                val yaUbicado = todasUbi.filter { it.loteDetalleId == det.id }.sumOf { it.cantidad }
                val stock = det.cantidad - yaUbicado
                if (stock > 0) {
                    LoteDetalleResponse(
                        id = det.id,
                        medicamentoId = det.medicamentoId,
                        medicamentoNombre = det.medicamentoNombre,
                        medicamentoPresentacion = det.medicamentoPresentacion,
                        fabricante = det.fabricante,
                        cantidad = stock,
                        precioUnitario = det.precioUnitario
                    )
                } else null
            }
            LoteConDetalles(lote = lote, detallesDisponibles = detallesCalc)
        }.filter { loteCon ->
            val term = search.lowercase().trim()
            if (term.isEmpty()) true
            else {
                loteCon.lote.factura?.lowercase()?.contains(term) == true ||
                        loteCon.detallesDisponibles.any { det ->
                            medicamentos.firstOrNull { it.id == det.medicamentoId }?.nombre?.lowercase()?.contains(term) == true
                        }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ====================== CARGAR DATOS INICIALES ======================
    fun cargarDatosBase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val racksRes = api.obtenerRacks()
                val lotesRes = api.obtenerLotes()
                val medsRes = api.obtenerMedicamentos()
                val todasUbiRes = api.obtenerUbicaciones()
                _racks.value = racksRes.body() ?: emptyList()
                _lotes.value = lotesRes.body() ?: emptyList()
                _medicamentos.value = medsRes.body() ?: emptyList()
                _todasUbicaciones.value = todasUbiRes.body() ?: emptyList()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seleccionarRack(rackId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rackRes = api.obtenerRack(rackId)
                if (rackRes.isSuccessful) {
                    _rackSeleccionado.value = rackRes.body()
                    val ubicRes = api.obtenerUbicacionesPorRack(rackId)
                    if (ubicRes.isSuccessful) {
                        _ubicacionesRack.value = ubicRes.body() ?: emptyList()
                    }
                }
                resetearSeleccion()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ====================== MANEJO DE CELDAS ======================
    fun onCeldaClick(nivel: Int, columna: Int, profundidadIndex: Int) {
        val ocupada = _ubicacionesRack.value.firstOrNull {
            it.nivel == nivel && it.columna == columna && it.profundidadIndex == profundidadIndex
        }

        if (_modoMovimiento.value && _origenMovimiento.value != null) {
            if (ocupada != null) return
            realizarMovimiento(
                origen = _origenMovimiento.value!!,
                destinoNivel = nivel,
                destinoCol = columna,
                destinoProf = profundidadIndex
            )
            return
        }

        if (ocupada != null) {
            _modoMovimiento.value = true
            _origenMovimiento.value = OrigenMovimiento(
                ubicacionId = ocupada.id,
                loteDetalleId = ocupada.loteDetalleId,
                cantidad = ocupada.cantidad,
                nivel = nivel,
                columna = columna,
                profundidadIndex = profundidadIndex,
                medicamentoNombre = obtenerNombreMedicamento(ocupada.loteDetalleId)
            )
            _celdaSeleccionada.value = null
        } else {
            _celdaSeleccionada.value = Celda(nivel, columna, profundidadIndex)
            _detalleSeleccionado.value = null
            _cantidad.value = 1
            _modoMovimiento.value = false
            _origenMovimiento.value = null
        }
    }

    private fun obtenerNombreMedicamento(loteDetalleId: Long): String {
        val lote = _lotes.value.firstOrNull { it.detalles.any { d -> d.id == loteDetalleId } }
        val det = lote?.detalles?.firstOrNull { it.id == loteDetalleId }
        val med = _medicamentos.value.firstOrNull { it.id == det?.medicamentoId }
        return med?.nombre ?: "Producto"
    }

    private fun realizarMovimiento(origen: OrigenMovimiento, destinoNivel: Int, destinoCol: Int, destinoProf: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.eliminarUbicacion(origen.ubicacionId)
                val request = UbicacionLoteRequest(
                    loteDetalleId = origen.loteDetalleId,
                    rackId = _rackSeleccionado.value!!.id,
                    nivel = destinoNivel,
                    columna = destinoCol,
                    profundidadIndex = destinoProf,
                    cantidad = origen.cantidad
                )
                api.asignarUbicacion(request)
                cargarDatosBase()
                seleccionarRack(_rackSeleccionado.value!!.id)
                resetearSeleccion()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelarMovimiento() {
        _modoMovimiento.value = false
        _origenMovimiento.value = null
    }

    fun asignarEnCascada() {
        val celda = _celdaSeleccionada.value ?: return
        val detalle = _detalleSeleccionado.value ?: return
        val rack = _rackSeleccionado.value ?: return
        val cantidad = _cantidad.value

        val stockDisponible = detalle.cantidad
        if (cantidad > stockDisponible) return

        viewModelScope.launch {
            _isLoading.value = true
            var restantes = cantidad
            var n = celda.nivel
            var c = celda.columna
            var p = celda.profundidadIndex
            val slots = mutableListOf<UbicacionLoteRequest>()

            while (restantes > 0) {
                val ocupada = _ubicacionesRack.value.any {
                    it.nivel == n && it.columna == c && it.profundidadIndex == p && it.activo
                }
                if (!ocupada) {
                    slots.add(
                        UbicacionLoteRequest(
                            loteDetalleId = detalle.id,
                            rackId = rack.id,
                            nivel = n,
                            columna = c,
                            profundidadIndex = p,
                            cantidad = 1
                        )
                    )
                    restantes--
                }
                p++
                if (p >= rack.profundidad) { p = 0; c++ }
                if (c >= rack.ancho) { c = 0; n++ }
                if (n >= rack.alto) break
            }

            try {
                slots.forEach { api.asignarUbicacion(it) }
                cargarDatosBase()
                seleccionarRack(rack.id)
                resetearSeleccion()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarUbicacion(ubicacionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.eliminarUbicacion(ubicacionId)
                cargarDatosBase()
                if (_rackSeleccionado.value != null) {
                    seleccionarRack(_rackSeleccionado.value!!.id)
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ====================== NUEVO RACK ======================
    fun actualizarNuevoRack(campo: String, valor: Any) {
        _nuevoRack.value = when (campo) {
            "nombre" -> _nuevoRack.value.copy(nombre = valor as String)
            "ancho" -> _nuevoRack.value.copy(ancho = valor as Int)
            "alto" -> _nuevoRack.value.copy(alto = valor as Int)
            "profundidad" -> _nuevoRack.value.copy(profundidad = valor as Int)
            else -> _nuevoRack.value
        }
    }

    fun crearRack(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val r = _nuevoRack.value
            try {
                api.crearRack(
                    RackRequest(
                        nombre = r.nombre,
                        ancho = r.ancho,
                        alto = r.alto,
                        profundidad = r.profundidad,
                        descripcion = null
                    )
                )
                cargarDatosBase()
                _showNuevoRackDialog.value = false
                _nuevoRack.value = NuevoRackState()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun eliminarRack(rackId: Long) {
        viewModelScope.launch {
            try {
                api.eliminarRack(rackId)
                if (_rackSeleccionado.value?.id == rackId) {
                    _rackSeleccionado.value = null
                    _ubicacionesRack.value = emptyList()
                }
                cargarDatosBase()
            } catch (_: Exception) {
            }
        }
    }

    // ====================== MÉTODOS PÚBLICOS PARA MODIFICAR ESTADOS DESDE LA UI ======================
    fun setSearchTerm(term: String) { _searchTerm.value = term }
    fun abrirNuevoRackDialog() { _showNuevoRackDialog.value = true }
    fun cerrarNuevoRackDialog() { _showNuevoRackDialog.value = false }
    fun setDetalleSeleccionado(det: LoteDetalleResponse?) { _detalleSeleccionado.value = det }
    fun setCantidad(cant: Int) { _cantidad.value = cant }

    fun resetearSeleccion() {
        _celdaSeleccionada.value = null
        _detalleSeleccionado.value = null
        _cantidad.value = 1
        _searchTerm.value = ""
        _modoMovimiento.value = false
        _origenMovimiento.value = null
    }
}

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