package com.sanidad.movil.data.presentation.screens.lotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LotesViewModel : ViewModel() {
    private val api = NetworkModule.apiService

    // -------------------- Datos maestros --------------------
    private val _lotes = MutableStateFlow<List<LoteResponse>>(emptyList())
    val lotes: StateFlow<List<LoteResponse>> = _lotes

    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _proveedores = MutableStateFlow<List<ProveedorResponse>>(emptyList())
    val proveedores: StateFlow<List<ProveedorResponse>> = _proveedores

    private val _racks = MutableStateFlow<List<RackResponse>>(emptyList())
    val racks: StateFlow<List<RackResponse>> = _racks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // -------------------- Búsqueda y filtro --------------------
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filtroStock = MutableStateFlow("todos") // "todos", "stock", "agotado"
    val filtroStock: StateFlow<String> = _filtroStock

    // -------------------- Paginación --------------------
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage
    val rowsPerPage = 15

    // -------------------- Formulario --------------------
    private val _showSheet = MutableStateFlow(false)
    val showSheet: StateFlow<Boolean> = _showSheet

    private val _formData = MutableStateFlow(LoteFormState())
    val formData: StateFlow<LoteFormState> = _formData

    // -------------------- Carga inicial --------------------
    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lotesRes = api.obtenerLotes()
                val medsRes = api.obtenerMedicamentos()
                val provsRes = api.obtenerProveedores()
                val racksRes = api.obtenerRacks()

                _lotes.value = lotesRes.body()?.sortedByDescending { it.id } ?: emptyList()
                _medicamentos.value = medsRes.body() ?: emptyList()
                _proveedores.value = provsRes.body() ?: emptyList()
                _racks.value = racksRes.body() ?: emptyList()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------- Filtrado y paginación --------------------
    val lotesFiltrados: List<LoteResponse>
        get() {
            val query = _searchQuery.value.lowercase().trim()
            return _lotes.value.filter { l ->
                // Filtro por búsqueda
                val matchQuery = if (query.isEmpty()) true
                else {
                    val proveedorNombre = _proveedores.value.find { it.id == l.proveedorId }?.nombre?.lowercase() ?: ""
                    val facturaMatch = l.factura?.lowercase()?.contains(query) == true
                    val proveedorMatch = proveedorNombre.contains(query)
                    val medicamentoMatch = l.detalles?.any { det ->
                        val med = _medicamentos.value.find { it.id == det.medicamentoId }
                        med?.nombre?.lowercase()?.contains(query) == true
                    } ?: false
                    facturaMatch || proveedorMatch || medicamentoMatch
                }

                // Filtro por stock
                val totalStock = l.detalles?.sumOf { it.cantidad } ?: 0
                val stockMatch = when (_filtroStock.value) {
                    "stock" -> totalStock > 0 && l.activo
                    "agotado" -> totalStock == 0 && l.activo
                    else -> l.activo  // "todos" solo activos
                }

                matchQuery && stockMatch
            }
        }

    val totalPages: Int
        get() {
            val total = lotesFiltrados.size
            return if (total == 0) 1 else (total + rowsPerPage - 1) / rowsPerPage
        }

    val paginatedLotes: List<LoteResponse>
        get() {
            val start = (_currentPage.value - 1) * rowsPerPage
            val end = minOf(start + rowsPerPage, lotesFiltrados.size)
            if (start >= end) return emptyList()
            return lotesFiltrados.subList(start, end)
        }

    fun setSearch(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun setFiltro(filtro: String) {
        _filtroStock.value = filtro
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        if (page in 1..totalPages) _currentPage.value = page
    }

    // -------------------- Desactivar lote --------------------
    fun desactivarLote(id: Long, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                api.suspenderLote(id)
                onResult()
                cargarDatos()
            } catch (_: Exception) {}
        }
    }

    // -------------------- Abrir formulario nuevo --------------------
    fun abrirNuevo() {
        _formData.value = LoteFormState(
            fechaFabricacion = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            fechaVencimiento = "",
            proveedorId = null,
            factura = generarCodigoFactura(),
            detalles = mutableListOf(DetalleFormState())
        )
        _showSheet.value = true
    }

    fun cerrarSheet() {
        _showSheet.value = false
    }

    private fun generarCodigoFactura(): String {
        val ahora = LocalDate.now()
        val random = (1000..9999).random()
        return "FAC-${ahora.year}${String.format("%02d", ahora.monthValue)}-$random"
    }

    // -------------------- Manipulación de detalles --------------------
    fun addDetalle() {
        val nuevos = _formData.value.detalles.toMutableList()
        nuevos.add(DetalleFormState())
        _formData.value = _formData.value.copy(detalles = nuevos)
    }

    fun removeDetalle(index: Int) {
        val nuevos = _formData.value.detalles.toMutableList()
        if (nuevos.size > 1) {
            nuevos.removeAt(index)
            _formData.value = _formData.value.copy(detalles = nuevos)
        }
    }

    fun updateDetalle(index: Int, detalle: DetalleFormState) {
        val nuevos = _formData.value.detalles.toMutableList()
        if (index in nuevos.indices) {
            nuevos[index] = detalle
            _formData.value = _formData.value.copy(detalles = nuevos)
        }
    }

    fun updateCampo(campo: String, valor: Any) {
        _formData.value = when (campo) {
            "fechaFabricacion" -> _formData.value.copy(fechaFabricacion = valor as String)
            "fechaVencimiento" -> _formData.value.copy(fechaVencimiento = valor as String)
            "proveedorId" -> _formData.value.copy(proveedorId = valor as? Long)
            "factura" -> _formData.value.copy(factura = valor as String)
            else -> _formData.value
        }
    }

    // -------------------- Guardar lote --------------------
    fun guardarLote(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val form = _formData.value
            if (form.proveedorId == null || form.fechaVencimiento.isBlank()) {
                onError("Complete los campos obligatorios")
                return@launch
            }
            try {
                val request = LoteRequest(
                    fechaFabricacion = form.fechaFabricacion.ifBlank { null },
                    fechaVencimiento = form.fechaVencimiento,
                    proveedorId = form.proveedorId,
                    factura = form.factura.ifBlank { null },
                    detalles = form.detalles.map { d ->
                        LoteDetalleRequest(
                            medicamentoId = d.medicamentoId!!,
                            cantidad = d.cantidad,
                            rackId = d.rackId,
                            nivel = d.nivel,
                            columna = d.columna,
                            profundidadIndex = d.profundidadIndex
                        )
                    }
                )
                val response = api.crearLote(request)
                if (response.isSuccessful) {
                    onSuccess()
                    cargarDatos()
                } else {
                    onError("Error ${response.code()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }
}

data class LoteFormState(
    val fechaFabricacion: String = "",
    val fechaVencimiento: String = "",
    val proveedorId: Long? = null,
    val factura: String = "",
    val detalles: List<DetalleFormState> = listOf(DetalleFormState())
)

data class DetalleFormState(
    val medicamentoId: Long? = null,
    val cantidad: Int = 1,
    val rackId: Long? = null,
    val nivel: Int = 0,
    val columna: Int = 0,
    val profundidadIndex: Int = 0
)