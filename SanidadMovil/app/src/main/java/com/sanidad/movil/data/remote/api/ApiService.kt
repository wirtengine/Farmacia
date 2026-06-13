package com.sanidad.movil.data.remote.api

import com.sanidad.movil.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== AUTH ==========
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ========== MEDICAMENTOS ==========
    @GET("api/medicamentos")
    suspend fun obtenerMedicamentos(): Response<List<MedicamentoResponse>>

    @GET("api/medicamentos/{id}")
    suspend fun obtenerMedicamento(@Path("id") id: Long): Response<MedicamentoResponse>

    @POST("api/medicamentos")
    suspend fun crearMedicamento(@Body request: MedicamentoRequest): Response<MedicamentoResponse>

    @PUT("api/medicamentos/{id}")
    suspend fun actualizarMedicamento(@Path("id") id: Long, @Body request: MedicamentoRequest): Response<MedicamentoResponse>

    @DELETE("api/medicamentos/{id}")
    suspend fun desactivarMedicamento(@Path("id") id: Long): Response<Void>

    @PATCH("api/medicamentos/{id}/reactivar")
    suspend fun activarMedicamento(@Path("id") id: Long): Response<Void>

    @POST("api/medicamentos/{id}/imagen")
    suspend fun subirImagenMedicamento(@Path("id") id: Long, @Body file: okhttp3.MultipartBody.Part): Response<String>

    @GET("api/medicamentos/{id}/stock")
    suspend fun obtenerStockMedicamento(@Path("id") id: Long): Response<StockMedicamentoDTO>

    @GET("api/medicamentos/{id}/lotes")
    suspend fun obtenerLotesMedicamento(@Path("id") id: Long): Response<LotesMedicamentoDTO>

    // ========== CLIENTES ==========
    @GET("api/clientes")
    suspend fun obtenerClientes(): Response<List<ClienteResponse>>

    @GET("api/clientes/{id}")
    suspend fun obtenerCliente(@Path("id") id: Long): Response<ClienteResponse>

    @POST("api/clientes")
    suspend fun crearCliente(@Body request: ClienteRequest): Response<ClienteResponse>

    @PUT("api/clientes/{id}")
    suspend fun actualizarCliente(@Path("id") id: Long, @Body request: ClienteRequest): Response<ClienteResponse>

    @DELETE("api/clientes/{id}")
    suspend fun suspenderCliente(@Path("id") id: Long): Response<Void>

    @POST("api/clientes/{id}/abonar")
    suspend fun abonarSaldo(@Path("id") id: Long, @Query("monto") monto: Double): Response<ClienteResponse>

    // ========== VENTAS ==========
    @GET("api/ventas")
    suspend fun obtenerVentas(): Response<List<VentaResponse>>

    @GET("api/ventas/{id}")
    suspend fun obtenerVenta(@Path("id") id: Long): Response<VentaResponse>

    @POST("api/ventas")
    suspend fun crearVenta(@Body request: VentaRequest): Response<VentaResponse>

    @DELETE("api/ventas/{id}")
    suspend fun anularVenta(@Path("id") id: Long): Response<Void>

    // ========== USUARIOS ==========
    @POST("api/usuarios")
    suspend fun crearUsuario(@Body request: UsuarioRequest): Response<String>

    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): Response<List<UsuarioResponse>>

    @GET("api/usuarios/{id}")
    suspend fun obtenerUsuario(@Path("id") id: Long): Response<UsuarioResponse>

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Long, @Body request: ActualizarUsuarioRequest): Response<String>

    // ========== PROVEEDORES ==========
    @GET("api/proveedores")
    suspend fun obtenerProveedores(): Response<List<ProveedorResponse>>

    @GET("api/proveedores/{id}")
    suspend fun obtenerProveedor(@Path("id") id: Long): Response<ProveedorResponse>

    @POST("api/proveedores")
    suspend fun crearProveedor(@Body request: ProveedorRequest): Response<ProveedorResponse>

    @PUT("api/proveedores/{id}")
    suspend fun actualizarProveedor(@Path("id") id: Long, @Body request: ProveedorRequest): Response<ProveedorResponse>

    @DELETE("api/proveedores/{id}")
    suspend fun suspenderProveedor(@Path("id") id: Long): Response<Void>

    // ========== LOTES ==========
    @GET("api/lotes")
    suspend fun obtenerLotes(): Response<List<LoteResponse>>

    @GET("api/lotes/{id}")
    suspend fun obtenerLote(@Path("id") id: Long): Response<LoteResponse>

    @POST("api/lotes")
    suspend fun crearLote(@Body request: LoteRequest): Response<LoteResponse>

    @PUT("api/lotes/{id}")
    suspend fun actualizarLote(@Path("id") id: Long, @Body request: LoteRequest): Response<LoteResponse>

    @DELETE("api/lotes/{id}")
    suspend fun suspenderLote(@Path("id") id: Long): Response<Void>

    // ========== DEVOLUCIONES (cliente) ==========
    @POST("api/devoluciones/solicitar")
    suspend fun solicitarDevolucion(@Body request: DevolucionRequest): Response<DevolucionResponse>

    @PUT("api/devoluciones/aprobar")
    suspend fun aprobarDevolucion(@Body request: DevolucionAprobarRequest): Response<DevolucionResponse>

    @GET("api/devoluciones")
    suspend fun obtenerDevoluciones(): Response<List<DevolucionResponse>>

    @GET("api/devoluciones/{id}")
    suspend fun obtenerDevolucion(@Path("id") id: Long): Response<DevolucionResponse>

    // ========== DEVOLUCIONES A PROVEEDOR ==========
    @POST("api/devoluciones-proveedor/solicitar")
    suspend fun solicitarDevolucionProveedor(@Body request: DevolucionProveedorRequest): Response<DevolucionProveedorResponse>

    @PUT("api/devoluciones-proveedor/aprobar")
    suspend fun aprobarDevolucionProveedor(@Body request: DevolucionProveedorAprobarRequest): Response<DevolucionProveedorResponse>

    @GET("api/devoluciones-proveedor")
    suspend fun obtenerDevolucionesProveedor(): Response<List<DevolucionProveedorResponse>>

    @GET("api/devoluciones-proveedor/{id}")
    suspend fun obtenerDevolucionProveedor(@Path("id") id: Long): Response<DevolucionProveedorResponse>

    // ========== RACKS ==========
    @GET("api/racks")
    suspend fun obtenerRacks(): Response<List<RackResponse>>

    @GET("api/racks/{id}")
    suspend fun obtenerRack(@Path("id") id: Long): Response<RackResponse>

    @POST("api/racks")
    suspend fun crearRack(@Body request: RackRequest): Response<RackResponse>

    @PUT("api/racks/{id}")
    suspend fun actualizarRack(@Path("id") id: Long, @Body request: RackRequest): Response<RackResponse>

    @DELETE("api/racks/{id}")
    suspend fun eliminarRack(@Path("id") id: Long): Response<Void>

    // ========== RECETAS ==========
    @POST("api/recetas/upload")
    suspend fun subirReceta(
        @Query("codigoMinsa") codigoMinsa: String,
        @Query("farmaceuticoId") farmaceuticoId: Long,
        @Body file: okhttp3.MultipartBody.Part
    ): Response<RecetaResponse>

    @PUT("api/recetas/{recetaId}/validar")
    suspend fun validarReceta(
        @Path("recetaId") recetaId: Long,
        @Query("aprobar") aprobar: Boolean,
        @Query("farmaceuticoId") farmaceuticoId: Long?
    ): Response<RecetaResponse>

    @GET("api/recetas/{id}")
    suspend fun obtenerReceta(@Path("id") id: Long): Response<RecetaResponse>

    @GET("api/recetas/pendientes")
    suspend fun obtenerRecetasPendientes(): Response<List<RecetaResponse>>

    @GET("api/recetas/farmaceutico/{id}")
    suspend fun obtenerRecetasPorFarmaceutico(@Path("id") id: Long): Response<List<RecetaResponse>>

    @GET("api/recetas/disponibles")
    suspend fun obtenerRecetasDisponibles(): Response<List<RecetaResponse>>

    @GET("api/recetas")
    suspend fun obtenerTodasLasRecetas(): Response<List<RecetaResponse>>

    // ========== UBICACIONES ==========
    @GET("api/ubicaciones")
    suspend fun obtenerUbicaciones(): Response<List<UbicacionLoteResponse>>

    @GET("api/ubicaciones/rack/{rackId}")
    suspend fun obtenerUbicacionesPorRack(@Path("rackId") rackId: Long): Response<List<UbicacionLoteResponse>>

    @GET("api/ubicaciones/{id}")
    suspend fun obtenerUbicacion(@Path("id") id: Long): Response<UbicacionLoteResponse>

    @POST("api/ubicaciones")
    suspend fun asignarUbicacion(@Body request: UbicacionLoteRequest): Response<UbicacionLoteResponse>

    @DELETE("api/ubicaciones/{id}")
    suspend fun eliminarUbicacion(@Path("id") id: Long): Response<Void>

    // ========== DASHBOARD ==========
    @GET("api/dashboard/resumen")
    suspend fun obtenerDashboard(): Response<DashboardResponseDTO>

    // ========== ALERTS ==========
    @GET("api/alerts")
    suspend fun obtenerAlertas(): Response<List<AlertResponse>>

    @POST("api/alerts/{id}/acknowledge")
    suspend fun reconocerAlerta(@Path("id") id: Long): Response<Void>

    @POST("api/alerts/generate")
    suspend fun generarAlertas(): Response<Void>

    // ========== PERDIDAS ==========
    @GET("api/perdidas/vencidos")
    suspend fun obtenerProductosVencidos(): Response<List<ProductoVencidoDTO>>

    @GET("api/perdidas/inmoviles")
    suspend fun obtenerProductosInmoviles(): Response<List<ProductoInmovilDTO>>

    @GET("api/perdidas/inconsistencias")
    suspend fun obtenerInconsistenciasStock(): Response<List<InconsistenciaStockDTO>>

    @GET("api/perdidas/resumen")
    suspend fun obtenerResumenPerdidas(): Response<ResumenPerdidasDTO>

    // ========== RECOMMENDATIONS ==========
    @GET("api/recommendations")
    suspend fun obtenerRecomendaciones(): Response<List<RecommendationResponse>>

    @POST("api/recommendations/{id}/accept")
    suspend fun aceptarRecomendacion(@Path("id") id: Long): Response<Void>

    @POST("api/recommendations/{id}/dismiss")
    suspend fun descartarRecomendacion(@Path("id") id: Long): Response<Void>

    @POST("api/recommendations/generate")
    suspend fun generarRecomendaciones(): Response<Void>
}