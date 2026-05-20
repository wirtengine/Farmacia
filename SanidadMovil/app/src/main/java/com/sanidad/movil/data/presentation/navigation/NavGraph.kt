package com.sanidad.movil.data.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.data.presentation.screens.login.LoginScreen
import com.sanidad.movil.data.presentation.screens.dashboard.DashboardScreen
import com.sanidad.movil.data.presentation.screens.medicamentos.MedicamentosScreen
import com.sanidad.movil.data.presentation.screens.proveedores.ProveedoresScreen
import com.sanidad.movil.data.presentation.screens.lotes.LotesScreen
import com.sanidad.movil.data.presentation.screens.ubicaciones.UbicacionesScreen
import com.sanidad.movil.data.presentation.screens.devoluciones.DevolucionesScreen
import com.sanidad.movil.presentation.screens.clientes.ClientesScreen
import com.sanidad.movil.presentation.screens.usuarios.UsuariosScreen
import com.sanidad.movil.presentation.screens.devolucionesProveedor.DevolucionesProveedorScreen
import com.sanidad.movil.presentation.screens.racks.RacksScreen
import com.sanidad.movil.presentation.screens.recetas.RecetasScreen
import com.sanidad.movil.presentation.screens.alerts.AlertsScreen
import com.sanidad.movil.presentation.screens.perdidas.PerdidasScreen
import com.sanidad.movil.presentation.screens.recommendations.RecommendationsScreen
import com.sanidad.movil.data.presentation.screens.ventas.VentasScreen
import com.sanidad.movil.data.presentation.screens.ventas.VentaCreateScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {

    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val tokenDataStore = TokenDataStore(MyApplication.instance)
    val authRepository = AuthRepository(tokenDataStore = tokenDataStore)

    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenDataStore.tokenFlow.first()
        NetworkModule.setToken(token)
        isLoggedIn = token != null
    }

    if (isLoggedIn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isLoggedIn == true) "dashboard" else "login"

    val drawerItems = listOf(
        "Dashboard" to "dashboard",
        "Medicamentos" to "medicamentos",
        "Clientes" to "clientes",
        "Ventas" to "ventas",
        "Usuarios" to "usuarios",
        "Proveedores" to "proveedores",
        "Lotes" to "lotes",
        "Devoluciones" to "devoluciones",
        "Dev. Proveedor" to "devoluciones_proveedor",
        "Racks" to "racks",
        "Recetas" to "recetas",
        "Ubicaciones" to "ubicaciones",
        "Alertas" to "alertas",
        "Pérdidas" to "perdidas",
        "Recomendaciones" to "recomendaciones"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Farmacia Sanidad",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                HorizontalDivider()

                drawerItems.forEach { (label, route) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate(route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            authRepository.logout()

                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isLoggedIn == true) {
                    TopAppBar(
                        title = { Text("Sanidad") },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú"
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {

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
                        onNavigateAlerts = {
                            navController.navigate("alertas")
                        },
                        onNavigateRecommendations = {
                            navController.navigate("recomendaciones")
                        },
                        onLogout = {
                            coroutineScope.launch {
                                authRepository.logout()

                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable("medicamentos") { MedicamentosScreen() }
                composable("clientes") { ClientesScreen() }

                composable("ventas") {
                    VentasScreen(
                        onNuevaVenta = {
                            navController.navigate("venta_create")
                        }
                    )
                }

                composable("venta_create") {
                    VentaCreateScreen(
                        usuarioId = 1L,
                        onVentaExitosa = {
                            navController.popBackStack()
                        },
                        onCancelar = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("usuarios") { UsuariosScreen() }
                composable("proveedores") { ProveedoresScreen() }
                composable("lotes") { LotesScreen() }

                composable("devoluciones") {
                    DevolucionesScreen()
                }

                composable("devoluciones_proveedor") {
                    DevolucionesProveedorScreen()
                }

                composable("racks") { RacksScreen() }
                composable("recetas") { RecetasScreen() }
                composable("ubicaciones") { UbicacionesScreen() }
                composable("alertas") { AlertsScreen() }
                composable("perdidas") { PerdidasScreen() }
                composable("recomendaciones") { RecommendationsScreen() }
            }
        }
    }
}