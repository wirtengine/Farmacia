package com.sanidad.movil.data.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.data.presentation.screens.login.LoginScreen
import com.sanidad.movil.presentation.screens.dashboard.DashboardScreen
import com.sanidad.movil.presentation.screens.medicamentos.MedicamentosScreen
import com.sanidad.movil.presentation.screens.clientes.ClientesScreen
import com.sanidad.movil.presentation.screens.ventas.VentasScreen
import com.sanidad.movil.presentation.screens.usuarios.UsuariosScreen
import com.sanidad.movil.presentation.screens.proveedores.ProveedoresScreen
import com.sanidad.movil.presentation.screens.lotes.LotesScreen
import com.sanidad.movil.presentation.screens.devoluciones.DevolucionesScreen
import com.sanidad.movil.presentation.screens.devolucionesProveedor.DevolucionesProveedorScreen
import com.sanidad.movil.presentation.screens.racks.RacksScreen
import com.sanidad.movil.presentation.screens.recetas.RecetasScreen
import com.sanidad.movil.presentation.screens.ubicaciones.UbicacionesScreen
import com.sanidad.movil.presentation.screens.alerts.AlertsScreen
import com.sanidad.movil.presentation.screens.perdidas.PerdidasScreen
import com.sanidad.movil.presentation.screens.recommendations.RecommendationsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val tokenDataStore = TokenDataStore(MyApplication.instance)
    val authRepository = AuthRepository(tokenDataStore = tokenDataStore)
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenDataStore.tokenFlow.first()
        NetworkModule.setToken(token)
        isLoggedIn = token != null
    }

    if (isLoggedIn == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isLoggedIn == true) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                authRepository = authRepository
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onLogout = {
                    coroutineScope.launch {
                        authRepository.logout()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("medicamentos") { MedicamentosScreen() }
        composable("clientes") { ClientesScreen() }
        composable("ventas") { VentasScreen() }
        composable("usuarios") { UsuariosScreen() }
        composable("proveedores") { ProveedoresScreen() }
        composable("lotes") { LotesScreen() }
        composable("devoluciones") { DevolucionesScreen() }
        composable("devoluciones_proveedor") { DevolucionesProveedorScreen() }
        composable("racks") { RacksScreen() }
        composable("recetas") { RecetasScreen() }
        composable("ubicaciones") { UbicacionesScreen() }
        composable("alertas") { AlertsScreen() }
        composable("perdidas") { PerdidasScreen() }
        composable("recomendaciones") { RecommendationsScreen() }
    }
}