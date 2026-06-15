package com.sanidad.movil.data.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sanidad.movil.data.repository.*
import com.sanidad.movil.presentation.screens.alerts.AlertsViewModel
import com.sanidad.movil.presentation.screens.clientes.ClientesViewModel
import com.sanidad.movil.presentation.screens.dashboard.DashboardViewModel
import com.sanidad.movil.presentation.screens.devoluciones.DevolucionesViewModel
import com.sanidad.movil.presentation.screens.devolucionesProveedor.DevolucionesProveedorViewModel
import com.sanidad.movil.presentation.screens.login.LoginViewModel
import com.sanidad.movil.presentation.screens.lotes.LotesViewModel
import com.sanidad.movil.presentation.screens.medicamentos.MedicamentosViewModel
import com.sanidad.movil.presentation.screens.perdidas.PerdidasViewModel
import com.sanidad.movil.presentation.screens.proveedores.ProveedoresViewModel
import com.sanidad.movil.presentation.screens.racks.RacksViewModel
import com.sanidad.movil.presentation.screens.recetas.RecetasViewModel
import com.sanidad.movil.presentation.screens.recommendations.RecommendationsViewModel
import com.sanidad.movil.presentation.screens.ubicaciones.UbicacionesViewModel
import com.sanidad.movil.presentation.screens.usuarios.UsuariosViewModel
import com.sanidad.movil.presentation.screens.ventas.VentaCreateViewModel
import com.sanidad.movil.presentation.screens.ventas.VentasViewModel

object AppViewModelFactory {

    // ── Dashboard ──
    fun dashboard() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                DashboardRepository(),
                AlertRepository(),
                RecommendationRepository()
            ) as T
        }
    }

    // ── Login ──
    fun login(authRepo: AuthRepository) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(authRepo) as T
        }
    }

    // ── Alertas ──
    fun alerts() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertsViewModel(AlertRepository()) as T
        }
    }

    // ── Clientes ──
    fun clientes() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientesViewModel(ClienteRepository()) as T
        }
    }

    // ── Devoluciones ──
    fun devoluciones() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DevolucionesViewModel(
                DevolucionRepository(),
                MedicamentoRepository(),
                LoteRepository(),
                VentaRepository()
            ) as T
        }
    }

    // ── Devoluciones Proveedor ──
    fun devolucionesProveedor() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DevolucionesProveedorViewModel(
                DevolucionProveedorRepository(),
                MedicamentoRepository(),
                ProveedorRepository(),
                LoteRepository()
            ) as T
        }
    }

    // ── Lotes ──
    fun lotes() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LotesViewModel(
                LoteRepository(),
                MedicamentoRepository(),
                ProveedorRepository(),
                RackRepository()
            ) as T
        }
    }

    // ── Medicamentos ──
    fun medicamentos() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicamentosViewModel(MedicamentoRepository()) as T
        }
    }

    // ── Pérdidas ──
    fun perdidas() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PerdidasViewModel(PerdidasRepository()) as T
        }
    }

    // ── Proveedores ──
    fun proveedores() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProveedoresViewModel(ProveedorRepository()) as T
        }
    }

    // ── Racks ──
    fun racks() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RacksViewModel(RackRepository()) as T
        }
    }

    // ── Recetas ──
    fun recetas() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecetasViewModel(RecetaRepository()) as T
        }
    }

    // ── Recomendaciones ──
    fun recommendations() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecommendationsViewModel(RecommendationRepository()) as T
        }
    }

    // ── Ubicaciones ──
    fun ubicaciones() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UbicacionesViewModel(
                RackRepository(),
                UbicacionRepository(),
                LoteRepository(),
                MedicamentoRepository()
            ) as T
        }
    }

    // ── Usuarios ──
    fun usuarios() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UsuariosViewModel(UsuarioRepository()) as T
        }
    }

    // ── Ventas (lista) ──
    fun ventas() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VentasViewModel(VentaRepository()) as T
        }
    }

    // ── Venta Create (POS) ──
    fun ventaCreate() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VentaCreateViewModel(
                MedicamentoRepository(),
                ClienteRepository(),
                LoteRepository(),
                RecetaRepository(),
                VentaRepository()
            ) as T
        }
    }
}