package com.sanidad.movil.presentation.screens.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanidad.movil.data.UserSession
import com.sanidad.movil.data.remote.dto.VentaResponse
import java.text.NumberFormat
import java.util.Locale

private val Primary  = Color(0xFF3B82F6)
private val Success  = Color(0xFF16A34A)
private val Danger   = Color(0xFFEF4444)
private val Slate50  = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val White    = Color.White

@Composable
fun VentasScreen(
    ventasViewModel: VentasViewModel,          // ← inyectado desde NavHost con factory
    onNuevaVenta: (onVentaCreada: (VentaResponse) -> Unit) -> Unit = {}
) {
    val state by ventasViewModel.uiState.collectAsState()
    val esAdmin = UserSession.isAdmin()

    Scaffold(containerColor = Slate50) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Header ──────────────────────────────────────────────────────
            VentasHeader(
                searchQuery      = state.searchQuery,
                onSearchChange   = { ventasViewModel.setSearch(it) },
                empleados        = ventasViewModel.empleados,
                empleadoFiltrado = state.empleadoFiltrado,
                onEmpleadoSelected = { ventasViewModel.setEmpleadoFiltrado(it) },
                esAdmin          = esAdmin,
                onNuevaVenta     = {
                    // Al cerrar VentaCreateScreen con éxito, refrescamos la lista
                    onNuevaVenta { nuevaVenta ->
                        ventasViewModel.agregarVentaYRecargar(nuevaVenta)
                    }
                }
            )

            // ── Banner de error ──────────────────────────────────────────────
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Danger)
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { ventasViewModel.limpiarError() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Danger)
                            }
                        }
                    }
                }
            }

            // ── Tabla / Cards ────────────────────────────────────────────────
            val wideMode = LocalConfiguration.current.screenWidthDp > 600

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    when {
                        state.isLoading && state.ventas.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        state.ventas.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("No se encontraron ventas.", color = Slate400)
                                }
                            }
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                if (wideMode) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Slate50)
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            TableHeader("Factura",      Modifier.weight(1f))
                                            TableHeader("Vendedor",     Modifier.weight(1.2f))
                                            TableHeader("Cliente",      Modifier.weight(1.5f))
                                            TableHeader("Medicamentos", Modifier.weight(3f))
                                            TableHeader("Total",        Modifier.weight(1f))
                                            TableHeader("Acción",       Modifier.weight(0.8f), alignEnd = true)
                                        }
                                        HorizontalDivider(color = Slate200)
                                    }
                                }

                                itemsIndexed(
                                    items = ventasViewModel.paginatedVentas,
                                    key   = { _, v -> v.id }
                                ) { _, venta ->
                                    if (wideMode) VentaRow(venta)
                                    else VentaCardCompact(venta)
                                }

                                if (state.totalPages > 1) {
                                    item {
                                        HorizontalDivider(color = Slate200)
                                        PaginationBar(
                                            currentPage = state.currentPage,
                                            totalPages  = state.totalPages,
                                            onPage      = { ventasViewModel.setPage(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun VentasHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    empleados: List<String>,
    empleadoFiltrado: String?,
    onEmpleadoSelected: (String?) -> Unit,
    esAdmin: Boolean,
    onNuevaVenta: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Ventas", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar factura o cliente...", fontSize = 14.sp, color = Slate400) },
                leadingIcon  = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine   = true,
                shape        = RoundedCornerShape(40.dp),
                colors       = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate200,
                    focusedBorderColor   = Primary
                )
            )
            if (esAdmin && empleados.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        shape   = RoundedCornerShape(40.dp),
                        colors  = ButtonDefaults.textButtonColors(
                            containerColor = if (empleadoFiltrado != null) Primary else Slate100,
                            contentColor   = if (empleadoFiltrado != null) White else Slate500
                        )
                    ) {
                        Text(empleadoFiltrado ?: "Vendedores", fontSize = 14.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text    = { Text("Ver todos") },
                            onClick = { onEmpleadoSelected(null); expanded = false }
                        )
                        empleados.forEach { emp ->
                            DropdownMenuItem(
                                text    = { Text(emp) },
                                onClick = { onEmpleadoSelected(emp); expanded = false }
                            )
                        }
                    }
                }
            }
            Button(
                onClick          = onNuevaVenta,
                shape            = RoundedCornerShape(40.dp),
                colors           = ButtonDefaults.buttonColors(containerColor = Slate900),
                contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("＋ Nueva Venta", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun TableHeader(text: String, modifier: Modifier, alignEnd: Boolean = false) {
    Text(
        text      = text.uppercase(),
        modifier  = modifier,
        fontSize  = 11.sp,
        fontWeight = FontWeight.Bold,
        color     = Slate500,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun VentaRow(venta: VentaResponse) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("#${venta.numeroFactura}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Box(modifier = Modifier.weight(1.2f)) {
                Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.1f)) {
                    Text(
                        venta.usuarioUsername,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color      = Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp
                    )
                }
            }

            Text(
                venta.clienteNombre ?: "Consumidor Final",
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                color    = Slate700
            )

            Row(modifier = Modifier.weight(3f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                venta.detalles.take(3).forEach { det ->
                    Surface(
                        shape  = RoundedCornerShape(8.dp),
                        color  = Slate100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Text(
                            "${det.medicamentoNombre} x${det.cantidad}",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (venta.detalles.size > 3) {
                    Text("+${venta.detalles.size - 3}", fontSize = 12.sp, color = Slate400)
                }
            }

            Text(
                formatCurrency(venta.total ?: 0.0),
                modifier   = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color      = Success,
                fontSize   = 14.sp,
                textAlign  = TextAlign.End
            )

            Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Print, null, tint = Slate400, modifier = Modifier.size(24.dp))
            }
        }
        HorizontalDivider(color = Slate100)
    }
}

@Composable
private fun VentaCardCompact(venta: VentaResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            // Total en esquina superior derecha
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape    = RoundedCornerShape(20.dp),
                color    = Success.copy(alpha = 0.1f)
            ) {
                Text(
                    formatCurrency(venta.total ?: 0.0),
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Success
                )
            }

            Column {
                // Factura + Cliente
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${venta.numeroFactura}", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(venta.clienteNombre ?: "Consumidor Final", fontSize = 13.sp, color = Slate500)
                }

                Spacer(Modifier.height(4.dp))

                // Vendedor chip
                Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.1f)) {
                    Text(
                        venta.usuarioUsername,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color      = Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 11.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Medicamentos chips
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    venta.detalles.take(3).forEach { det ->
                        Surface(
                            shape  = RoundedCornerShape(8.dp),
                            color  = Slate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                "${det.medicamentoNombre} x${det.cantidad}",
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (venta.detalles.size > 3) {
                        Text("+${venta.detalles.size - 3}", fontSize = 11.sp, color = Slate400)
                    }
                }

                // Acción imprimir
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick  = { /* TODO: imprimir */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Print, null, tint = Slate400)
                    }
                }
            }
        }
    }
}

// ── Paginación ────────────────────────────────────────────────────────────────

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, onPage: (Int) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().background(Slate50).padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onPage(currentPage - 1) }, enabled = currentPage > 1) {
            Text("← Anterior")
        }
        val start = maxOf(1, currentPage - 2)
        val end   = minOf(totalPages, currentPage + 2)
        for (page in start..end) {
            val isActive = page == currentPage
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPage(page) },
                color    = if (isActive) Primary else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        page.toString(),
                        color      = if (isActive) White else Slate500,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        TextButton(onClick = { onPage(currentPage + 1) }, enabled = currentPage < totalPages) {
            Text("Siguiente →")
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "NI")).format(value)